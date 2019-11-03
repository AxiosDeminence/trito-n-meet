import psycopg2
import base64

import secrets
from argon2 import PasswordHasher

import falcon

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
        try:
            email = strip(req.media.get("email"))
            full_name = strip(req.media.get("fullName"))
            password = strip(req.media.get("password"))
            confirm_password = strip(req.media.get("confirmPassword"))
        except KeyError:
            resp.status = falcon.HTTP_400
            resp.media = {"message": "Not all forms filled"}
            return
        
        if password != confirm_password:
            resp.status = falcon.HTTP_406
            resp.media = {"message": "Passwords are not the same"}
            return
        elif not (re.match(r'[A-Z]') or re.match(r'[a-z]') or
                  re.match(r'[0-9]') or re.match(r'[!@#$%^&*()]')):
            resp.status = falcon.HTTP_406
            resp.media = {"message": "Does not meet password complexity"}
            return
        elif len(password) < 6 or len(password) > 30:
            resp.status = falcon.HTTP_406
            resp.media = {"mesage": "Password not of correct length"}
            return
        elif email[len(email) - 9:] != "@ucsd.edu":
            resp.status = falcon.HTTP_406
            resp.media = {"message": "Not a valid ucsd email"}
            return

        hasher = PasswordHasher()
        salt = str(base64.standard_b64encode(secrets.token_bytes(32)))
        hash = hasher.hash(password + salt)

        try:
            con = psycopg2.connect(credentials)
            with con:
                cur = con.cursor()
                cur.execute("""
                            insert into user_info(email,full_name,salt,hash)
                                values(%s, %s, %s, %s);""",
                            [email, full_name, salt, hash])
                con.commit()
        except psycopg2.OperationalError:
            resp.status = falcon.HTTP_503
            resp.media = {"message": "Connection terminated"}
            return

        resp.status = falcon.HTTP_201
        resp.media = {"message": "User created"}

class UserLogin(object):
    def on_post(self, req, resp):
        try:
            email = strip(req.media.get("email"))
            password = strip(req.media.get("password"))
        except KeyError:
            resp.status = falcon.HTTP_400
            resp.media = {"message": "Not all forms filled"}
            return

        hasher = PasswordHasher()
        user_info = None
        
        try:
            con = psycopg2.connect(credentials)        
            with con:
                cur = con.cursor()
                cur.execute("""
                            select salt, hash from user_info
                                where email = %s;
                            """, [email])
                user_info = cur.fetchone()
        except psycopg2.OperationalError:
            resp.status = falcon.HTTP_503
            resp.media = {"message": "Connection terminated"}
            return
        except psycopg2.ProgrammingError:
            resp.status = falcon.HTTP_400
            resp.media = {"message": "User does not exist"}
            return
            
        hash = hasher.hash(password + user_info[0])
        
        if hash != user_info[1]:
            resp.status = falcon.HTTP_401
            resp.media = {"message": "Passwords do not match"}
            return
        
        resp.status = falcon.HTTP_201
        resp.media = {"message": "User logged in"}

api = falcon.API()
createuser_endpoint = CreateUser()
userlogin_endpoint = UserLogin()
api.add_route("/createUser", createuser_endpoint)
api.add_route("/loginUser", userlogin_endpoint)
