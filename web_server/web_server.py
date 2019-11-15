import re

import falcon
import psycopg2
from argon2 import PasswordHasher

DATABASE = "d814roat3puk53"
USER = "evsifgooyevaft"
PASSWORD = "80f763bb1196c19be42f375323dedbfd6080cdec0605f12a689c5b51880505d2"
HOST = "ec2-107-20-243-220.compute-1.amazonaws.com"
PORT = "5432"

CREDENTIALS = ("dbname=%s user=%s password=%s host=%s port=%s"
               % (DATABASE, USER, PASSWORD, HOST, PORT))

class CreateUser():
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
        if not (re.match(r'[A-Z]', password)
                or re.match(r'[a-z]', password)
                or re.match(r'[0-9]', password)
                or re.match(r'[!@#$%^&*()]', password)):
            resp.status = falcon.HTTP_406
            resp.media = {"message": "Does not meet password complexity"}
            return
        if len(password) < 6 or len(password) > 30:
            resp.status = falcon.HTTP_406
            resp.media = {"mesage": "Password not of correct length"}
            return
        if not email.endswith("@ucsd.edu"):
            resp.status = falcon.HTTP_406
            resp.media = {"message": "Not a valid ucsd email"}
            return

        hasher = PasswordHasher()
        hash = hasher.hash(password)

        try:
            con = psycopg2.connect(CREDENTIALS)
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
        except psycopg2.errors.UniqueViolation:
            resp.status = falcon.HTTP_406
            resp.media = {"message": "User already exists"}
            return

        resp.status = falcon.HTTP_201
        resp.media = {"message": "User created"}

class UserLogin():
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
            con = psycopg2.connect(CREDENTIALS)
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

        resp.status = falcon.HTTP_200
        resp.media = {"message": "User logged in"}

class ManageEvents():
    def on_get(self, req, resp): # Ask for all events of a user
        try:
            user = str.strip(req.media.get("email"))
        except KeyError:
            resp.status = falcon.HTTP_400
            resp.media = {"message": "JSON Form Error"}
            return

        try:
            con = psycopg2.connect(CREDENTIALS)
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
        
        keys = ["eventID", "email", "eventName", "startTime", "endTime",
                "startDate", "endDate", "daysOfWeek"]
            
        events = [dict((key, value) for key, value in zip(x, keys))
                                    for x in events]
        print(events)

        for x in events:
            x["startTime"] = x["startTime"].strftime("%I:%M")
            x["endTime"] = x["endTime"].strftime("%I:%M")

        resp.status = falcon.HTTP_200
        resp.media = events

    def on_post(self, req, resp): # Will include edit and create
        try:
            action = str.strip(req.media.get("action"))

            if action in ("edit", "delete"):
                event_id = str.strip(req.media.get("eventId"))
            elif action == "create":
                pass
            else:
                raise KeyError("Not a valid action")

            user = str.strip(req.media.get("email"))
            event_name = str.strip(req.media.get("eventName"))
            start_time = str.strip(req.media.get("startTime"))
            end_time = str.strip(req.media.get("endTime"))
            start_date = str.strip(req.media.get("startDate"))
            end_date = str.strip(req.media.get("endDate"))
            days_of_week = list(map(str.strip, req.media.get("daysOfWeek")))
        except KeyError:
            resp.status = falcon.HTTP_400
            resp.media = {"message": "JSON Form Error"}
            return

        try:
            con = psycopg2.connect(CREDENTIALS)

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
                                insert into events(owner_email, event_name,
                                                      start_time, end_time,
                                                      start_date, end_date,
                                                      days_of_week)
                                    values(%s,%s,%s,%s,%s,%s,%s);""",
                                [user, event_name, start_time, end_time,
                                 start_date, end_date, days_of_week])
                elif action == "delete":
                    cur.execute("""
                                delete from events
                                    where event_id = %s and owner_email=%s;""",
                                [event_id, user])
                con.commit()
        except psycopg2.OperationalError:
            resp.status = falcon.HTTP_503
            resp.media = {"message": "Connection terminated"}
            return
        except psycopg2.ProgrammingError:
            resp.status = falcon.HTTP_400
            resp.media = {"message": "Event does not exist"}
            return

        resp.status = falcon.HTTP_200
        resp.media = {"message": "Event managed successfully"}

def ManageGroups():
    def on_get(self, req, resp):
        try:
            user = str.strip(req.media.get("email"))
        except KeyError:
            resp.status = falcon.HTTP_400
            resp.media = {"message": "JSON Form Error"}

        try:
            con = psycopg2.connect(CREDENTIALS)

            joined_groups = None
            invitations = None
            with con:
                cur = con.cursor()

                try:
                    cur.execute("""
                                select * from groups
                                    where %s = any(members);""",
                                [user])
                    joined_groups = cur.fetchall()
                except psycopg2.ProgrammingError:
                    pass

                try:
                    cur.execute("""
                                select * from groups
                                    where %s = any(invites);""",
                                [user])
                except psycopg2.ProgrammingError:
                    pass
        except psycopg2.OperationalError:
            resp.status = falcon.HTTP_503
            resp.media = {"message": "Connection terminated"}
            return

        resp.status = falcon.HTTP_201
        resp.media = {"groups": joined_groups if joined_groups is not None else [],
                      "invites": invitations if invitations is not None else []}

    def on_post(self, req, resp):
        try:
            action = str.strip(req.media.get("action"))

            group_name = str.strip(req.media.get("groupName"))
            creator_email = str.strip(req.media.get("owner"))
            if action in ("create", "delete"):
                pass
            if action == "invite":
                assert isinstance(req.media.get("users"), list), (
                    "Users are not encoded into a list")
                users = map(str.strip, req.media.get("users"))
            if action in ("join", "remove"):
                member_email = str.strip(req.media.get("email"))
        except (KeyError, AssertionError):
            resp.status = falcon.HTTP_400
            resp.media = {"message": "JSON Form Error"}
            return

        try:
            con = psycopg2.connect(CREDENTIALS)
            with con:
                cur = con.cursor()
                if action == "create":
                    cur.execute("""
                                select exists(select * from groups
                                    where group_name=%s and owner_email=%s);""",
                                [group_name, creator_email])
                    if cur.fetchone()[0]:
                        raise psycopg2.errors.UniqueViolation

                    cur.execute("""
                                insert into groups(group_name, owner_email,
                                                   members, invites)
                                    values(%s, %s, %s, %s);""",
                                [group_name, creator_email, [], []])

                    resp.status = falcon.HTTP_200
                    resp.media = {"message": "Group created"}
                    return

                if action == "delete":
                    cur.execute("""
                                delete from groups
                                    where group_name = %s and owner_email = %s;
                                """, [group_name, creator_email])

                    resp.status = falcon.HTTP_200
                    resp.media = {"message": "Group deleted"}
                    return

                if action == "invite":
                    invalid_users = []
                    for x in users:
                        cur.execute("""
                                    select exists(select * from user_info
                                        where email=%s);""",
                                    [x])
                        if not cur.fetchone()[0]:
                            invalid_users.append(x)

                    users = [x for x in users if x not in invalid_users]
                    for x in users:
                        cur.execute("""
                                    select exists(select * from groups
                                        where group_name=%s and owner_email=%s
                                        and (%s=any(invitations) or
                                             %s=any(members)));""",
                                    [group_name, creator_email, x])
                        if cur.fetchone()[0]:
                            invalid_users.append(x)
                    users = [x for x in users if x not in invalid_users]

                    cur.execute("""
                                update groups
                                    set invites=array_cat(invites,%s)
                                    where group_name=%s and owner_email=%s;""",
                                [users, group_name, creator_email])

                    resp.status = falcon.HTTP_200
                    response = {"message": "Attempted inviting users",
                                "valid_invitations": users}
                    if len(invalid_users) != 0:
                        response["invalid_invitations"] = invalid_users
                    resp.media = response
                    return

                if action == "join":
                    cur.execute("""
                                select exists(select * from user_info
                                   where email=%s);""", [member_email])
                    if not cur.fetchone()[0]:
                        resp.status = falcon.HTTP_400
                        resp.media = {"message": "User does not exist"}
                        return

                    cur.execute("""
                                select exists(select * from groups
                                    where group_name=%s and owner_email=%s
                                    and %s=any(members));""",
                                [group_name, creator_email, member_email])
                    if cur.fetchone()[0]:
                        resp.status = falcon.HTTP_400
                        resp.media = {"message": "User already in group"}
                        return

                    cur.execute("""
                                select exists(select * from groups
                                    where group_name=%s and owner_email=%s
                                    and %s=any(invitations));""",
                                [group_name, creator_email, member_email])
                    if not cur.fetchone()[0]:
                        resp.status = falcon.HTTP_403
                        resp.media = {"message": "User not invited to group"}
                        return

                    cur.execute("""
                                update groups
                                    set invites=array_remove(invites,%s)
                                    where group_name=%s and owner_email=%s;""",
                                [member_email, group_name, creator_email])
                    cur.execute("""
                                update groups
                                    set members=array_cat(members,%s)
                                    where group_name=%s and owner_email=%s;""",
                                [member_email, group_name, creator_email])

                    resp.status = falcon.HTTP_200
                    resp.media = {"message": "Joined group successfully"}

                if action == "remove":
                    if member_email == creator_email:
                        resp.status = falcon.HTTP_400
                        resp.media = {"message": "Cannot remove owner"}
                        return

                    cur.execute("""
                                select exists(select * from groups
                                    where group_name=%s and owner_email=%s
                                    and %s=any(members));""",
                                [group_name, creator_email, member_email])
                    if not cur.fetchone()[0]:
                        resp.status = falcon.HTTP_400
                        resp.media = {"message": "User not in group"}
                        return

                    cur.execute("""
                                update groups
                                    set members=array_remove(members,%s)
                                    where group_name=%s and owner_email=%s;""",
                                [member_email, group_name, creator_email])

                    resp.status = falcon.HTTP_200
                    resp.media = {"message": "User left group successfully"}
        except psycopg2.OperationalError:
            resp.status = falcon.HTTP_503
            resp.media = {"message": "Connection terminated"}
            return
        except psycopg2.ProgrammingError:
            resp.status = falcon.HTTP_400
            resp.media = {"message": "Event does not exist"}
            return

API = falcon.API()
CREATEUSER_ENDPOINT = CreateUser()
USERLOGIN_ENDPOINT = UserLogin()
MANAGEEVENTS_ENDPOINT = ManageEvents()
MANAGEGROUPS_ENDPOINT = ManageGroups()
API.add_route("/createUser", CREATEUSER_ENDPOINT)
API.add_route("/loginUser", USERLOGIN_ENDPOINT)
API.add_route("/manageEvents", MANAGEEVENTS_ENDPOINT)
API.add_route("/manageGroups", MANAGEGROUPS_ENDPOINT)
