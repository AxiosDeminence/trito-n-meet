from contextlib import closing

import falcon
import psycopg2
import argon2


class UserLogin:
    _CREDENTIALS = None

    def __init__(self, credentials):
        UserLogin._CREDENTIALS = credentials

    @staticmethod
    def form_parser(form):
        is_valid = True
        try:
            email = str.strip(form.get("email"))
            password = str.strip(form.get("password"))
        except (KeyError, TypeError):
            is_valid = False
            result = "JSON Form Error"
        else:
            result = {"email": email, "password": password}

        return {"is_valid": is_valid, "result": result}

    @staticmethod
    def grab_user_hash(user):
        is_success = False
        try:
            with closing(psycopg2.connect(UserLogin._CREDENTIALS)) as con:
                with con, con.cursor() as cur:
                    cur.execute("""
                                select hash from user_info
                                    where email=%s;""",
                                [user])
                    user_hash = cur.fetchone()[0]
        except psycopg2.OperationalError:
            result = falcon.HTTP_503, "Generic database error"
        except psycopg2.ProgrammingError:
            result = falcon.HTTP_406, "User does not exist"
        else:
            is_success = True
            result = user_hash

        return {"is_success": is_success, "result": result}

    @staticmethod
    def verify_identify(password, user_hash):
        try:
            argon2.PasswordHasher().hasher.verify(password, user_hash)
        except argon2.exceptions.VerifyMismatchError:
            is_valid = False
            result = falcon.HTTP_401, "Incorrect password"
        else:
            is_valid = True
            result = falcon.HTTP_200, "User logged in"

        return {"is_valid": is_valid, "result": result}

    def on_post(self, req, resp):
        form = UserLogin.form_parser(req.media)
        if not form.get("is_valid"):
            resp.status = falcon.HTTP_406
            resp.media = {"message": form.get("result")}
            return

        form = form.get("result")
        user_hash = UserLogin.grab_user_hash(form.get("email"))

        if not user_hash.get("is_success"):
            resp.status = user_hash.get("result")[0]
            resp.media = {"message": user_hash.get("result")[1]}
            return

        user_hash = user_hash.get("result")

        verification = UserLogin.verify_identity(form.get("password"),
                                                 user_hash)
        resp.status = verification.get("result")[0]
        resp.media = {"message": verification.get("result")[1]}
