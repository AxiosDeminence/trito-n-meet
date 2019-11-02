import psycopg2
import base64

import secrets
from argon2 import PasswordHasher

import falcon
import json

import re

database = "d814roat3puk53"
user = "evsifgooyevaft"
password = "80f763bb1196c19be42f375323dedbfd6080cdec0605f12a689c5b51880505d2"
host = "ec2-107-20-243-220.compute-1.amazonaws.com"
port = "5432"

credentials = ("dbname=%s user=%s password=%s host=%s port=%s"
               % (database, user, password, host, port))

class CreateUser(object):
    def on_post(self, req, resp):        
        email = strip(req.media.get("email"))
        full_name = strip(req.media.get("fullName"))
        password = strip(req.media.get("password"))
        confirm_password = strip(req.media.get("confirmPassword"))
        
        if password != confirm_password:
            resp.status = falcon.HTTP_406
            resp.media = {"message": "Passwords are not the same"}
        elif not (re.match(r'[A-Z]') or re.match(r'[a-z]') or
                  re.match(r'[0-9]') or re.match(r'[!@#$%^&*()]')):
            resp.status = falcon.HTTP_406
            resp.media = {"message": "Does not meet password complexity"}
        elif len(password) < 6 or len(password) > 30:
            resp.status = falcon.HTTP_406
            resp.media = {"mesage": "Password not of correct length"}
        elif email[len(email) - 9:] != "@ucsd.edu":
            resp.status = falcon.HTTP_406
            resp.media = {"message": "Not a valid ucsd email"}
        else:
            con = psycopg2.connect(credentials)
            with con:
                hasher = PasswordHasher()
                
                salt = str(base64.standard_b64encode(secrets.token_bytes(32)))
                hash = hasher.hash(password + salt)
                
                cur = con.cursor()
                cur.execute("""
                            insert into user_info(email,full_name,salt,hash)
                                values(%s, %s, %s, %s);""",
                            [email, full_name, salt, hash])
                con.commit()

            resp.status = falcon.HTTP_201
            resp.body = json.dumps({"message": "User created"})



api = falcon.API()
createuser_endpoint = CreateUser()
api.add_route("/createUser", createuser_endpoint)
