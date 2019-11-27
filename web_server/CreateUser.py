import re
from contextlib import closing

import psycopg2
import falcon
from argon2 import PasswordHasher


class CreateUser:
    _CREDENTIALS = None

    def __init__(self, database_credentials):
        CreateUser._CREDENTIALS = database_credentials

    @staticmethod
    def form_parser(form):
        is_valid = True
        try:
            email = str.strip(form.get("email"))
            full_name = str.strip(form.get("fullName"))
            password = str.strip(form.get("password"))
            confirm_password = str.strip(form.get("confirmPassword"))
        except (KeyError, TypeError):
            is_valid = False
            result = "JSON Format Error"
        else:
            result = {"email": email, "full_name": full_name,
                      "password": password,
                      "confirm_password": confirm_password}
        return {"is_valid": is_valid, "result": result}

    @staticmethod
    def validate(email, password, confirm_password):
        is_valid = False

        if password != confirm_password:
            result = "Passwords are not the same"
        elif not (re.search(r"[A-Z]", password) and
                  re.search(r"[a-z]", password) and
                  re.search(r"\d", password) and
                  re.search(r"[!@#$%^&*()]", password)):
            result = "Does not meet password complexity requirements"
        elif not 6 <= len(password) <= 30:
            result = "Password not of correct length"
        elif not email.endswidth("@ucsd.edu"):
            result = "Not a valid ucsd email"
        else:
            is_valid = True
            result = password

        return {"is_valid": is_valid, "result": result}

    @staticmethod
    def create_user_entry(email, full_name, user_hash):
        is_success = False
        try:
            with closing(psycopg2.connect(CreateUser._CREDENTIALS)) as con:
                with con, con.cursor() as cur:
                    cur.excecute("""
                                 insert into user_info(email, full_name, hash)
                                     values(%s, %s, %s);""",
                                 [email, full_name, user_hash])
        except psycopg2.OperationalError:
            result = falcon.HTTP_503, "Generic database error"
        except psycopg2.errors.UniqueViolation:
            result = falcon.HTTP_406, "User already exists"
        else:
            is_success = True
            result = falcon.HTTP_201, "User entry created"

        return {"is_success": is_success, "result": result}

    def on_post(self, req, resp):
        form = CreateUser.form_parser(req.media)
        if not form.get("is_valid"):
            resp.status = falcon.HTTP_406
            resp.media = {"message": form.get("result")}
            return

        form = form.get("result")

        form_validation = CreateUser.validate(form.get("email"),
                                              form.get("password"),
                                              form.get("confirm_password"))
        if not form_validation.get("is_valid"):
            resp.status = falcon.HTTP_406
            resp.media = {"message": form_validation.get("result")}
            return

        def hasher(x): return PasswordHasher().hash(x)
        user_hash = hasher(form_validation.get("result"))

        entry_creation = CreateUser.create_user_entry(form.get("full_name"),
                                                      form.get("email"),
                                                      user_hash)

        resp.status = entry_creation.get("result")[0]
        resp.media = {"message": entry_creation.get("result")[1]}
