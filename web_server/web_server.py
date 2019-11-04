import psycopg2
import base64

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
            email = str.strip(req.media.get("email"))
            full_name = str.strip(req.media.get("fullName"))
            password = str.strip(req.media.get("password"))
            confirm_password = str.strip(req.media.get("confirmPassword"))
        except KeyError:
            resp.status = falcon.HTTP_400
            resp.media = {"message": "JSON Format Error"}
            return
        
        if password != confirm_password:
            resp.status = falcon.HTTP_406
            resp.media = {"message": "Passwords are not the same"}
            return
        elif not (re.match(r'[A-Z]', password)
                  or re.match(r'[a-z]', password)
                  or re.match(r'[0-9]', password)
                  or re.match(r'[!@#$%^&*()]')):
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
        hash = hasher.hash(password)

        try:
            con = psycopg2.connect(credentials)
            with con:
                cur = con.cursor()
                cur.execute("""
                            insert into user_info(email, full_name, hash)
                                values(%s, %s, %s);""",
                            [email, full_name, hash])
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
            email = str.strip(req.media.get("email"))
            password = str.strip(req.media.get("password"))
        except KeyError:
            resp.status = falcon.HTTP_400
            resp.media = {"message": "JSON Format Error"}
            return

        hasher = PasswordHasher()
        user_info = None
        
        try:
            con = psycopg2.connect(credentials)        
            with con:
                cur = con.cursor()
                cur.execute("""
                            select hash from user_info
                                where email = %s;""", [email])
                user_info = cur.fetchone()
        except psycopg2.OperationalError:
            resp.status = falcon.HTTP_503
            resp.media = {"message": "Connection terminated"}
            return
        except psycopg2.ProgrammingError:
            resp.status = falcon.HTTP_400
            resp.media = {"message": "User does not exist"}
            return
        
        try:
            hasher.verify(user_info[0], password)
        except argon2.exceptions.VerifyMisMatchError:
            resp.status = falcon.HTTP_401
            resp.media = {"message": "Incorrect password"}
            return
        
        resp.status = falcon.HTTP_201
        resp.media = {"message": "User logged in"}

class ManageEvents(object):
    def on_get(self, req, resp): # Ask for all events of a user
        try:
            user = str.strip(req.media.get("email"))
        except KeyError:
            resp.status = falcon.HTTP_400
            resp.media = {"message": "JSON Form Error"}
            return
        
        try:
            con = psycopg2.connect(credentials)
            with con:
                cur = con.cursor()
                cur.execute("""
                            select * from events
                                where owner_email = %s""",
                            [user])
                events = cur.fetchall()
        except psycopg2.OperationalError:
            resp.status = falcon.HTTP_503
            resp.media = {"message": "Connection terminated"}
            return
        except psycopg2.ProgrammingError:
            resp.status = falcon.HTTP_400
            resp.media = {"message": "User does not exist"}
            return

        resp.status = falcon.HTTP_201
        resp.media = events
    
    def on_post(self, req, resp): # Will include edit and create
        try:
            action = str.strip(req.media.get("action"))

            if action == "edit" or action == "delete":
                event_id = str.strip(req.media.get("eventId"))
            elif action == "create":
                pass
            else:
                raise KeyError("Neither editing nor creating")
            
            user = str.strip(req.media.get("email"))
            event_name = str.strip(req.media.get("eventName"))
            start_time = str.strip(req.media.get("startTime"))
            end_time = str.strip(req.media.get("endTime"))
            start_date = str.strip(req.media.get("startDate"))
            end_date = str.strip(req.media.get("endDate"))
            days_of_week = str.strip(req.media.get("daysOfWeek"))
        except KeyError:
            resp.status = falcon.HTTP_400
            resp.media = {"message": "JSON Form Error"}
        
        try:
            con = psycopg2.connect(credentials)
            
            with con:
                cur = con.cursor()
                if action == "edit":
                    cur.execute("""
                                update events set event_name=%s, start_time=%s,
                                                  end_time=%s, start_date=%s,
                                                  end_date=%s, days_of_week=%s
                                    where event_id = %s and owner_email=%s;""",
                                [event_name, start_time, end_time, start_date,
                                 end_date, days_of_week, event_id, user])
                elif action == "create":
                    cur.execute("""
                                insert into user_info(owner_email, event_name,
                                                      start_time, end_time,
                                                      start_date, end_date,
                                                      days_of_week)
                                    values(%s,%s,%s,%s,%s,%s);""",
                                [event_name, start_time, end_time, start_date,
                                 end_date, days_of_week])
                elif action == "delete":
                    cur.execute("""
                                delete from user_info
                                    where event_id = %s and owner_email=%s;""",
                                [event_id, owner_email])
                con.commit()
        except psycopg2.OperationalError: 
            resp.status = falcon.HTTP_503
            resp.media = {"message": "Connection terminated"}
            return
        except psycopg2.ProgrammingError:
            resp.status = falcon.HTTP_400
            resp.media = {"message": "Event does not exist"}
            return
        
        resp.status = falcon.HTTP_201
        resp.media = {"message": "Event managed successfully"}

api = falcon.API()
createuser_endpoint = CreateUser()
userlogin_endpoint = UserLogin()
api.add_route("/createUser", createuser_endpoint)
api.add_route("/loginUser", userlogin_endpoint)
