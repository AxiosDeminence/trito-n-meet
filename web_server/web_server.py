import psycopg2
import base64

import secrets
from argon2 import PasswordHasher

import falcon
import json

database = "d814roat3puk53"
user = "evsifgooyevaft"
password = "80f763bb1196c19be42f375323dedbfd6080cdec0605f12a689c5b51880505d2"
host = "ec2-107-20-243-220.compute-1.amazonaws.com"
port = "5432"

credentials = ("dbname=%s user=%s password=%s host=%s port=%s"
               % (database, user, password, host, port))

class CreateUser(object):
    def on_post(self, req, resp):        
        email = req.media.get("email")
        full_name = req.media.get("fullName")
        password = req.media.get("password")
        confirm_password = req.media.get("confirmPassword")
        
        if (password != confirm_password):
            resp.status = falcon.HTTP_401
            resp.media = {"message": "Passwords are not the same"}
        else:
            con = psycopg2.connect(credentials)            
            hasher = PasswordHasher()
            
            salt = base64.standard_b64encode(secrets.token_bytes(32))
            hash = hasher.hash(password + salt)

            """with con:
                cur = con.cursor()
                cur.execute("""
                            insert into user_info(email,full_name,salt,hash)
                                values(%s, %s, %s, %s);""",
                            [email, fullname, salt, hash])
                con.commit()"""

            resp.status = falcon.HTTP_201
            resp.media = {"message": "User created"}

api = falcon.API()
createuser_endpoint = CreateUser()
api.add_route("/createUser", createuser_endpoint)
