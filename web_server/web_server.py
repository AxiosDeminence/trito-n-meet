import re
import datetime

import falcon
import psycopg2
import argon2
from argon2 import PasswordHasher

DATABASE = "d814roat3puk53"
USER = "evsifgooyevaft"
PASSWORD = "80f763bb1196c19be42f375323dedbfd6080cdec0605f12a689c5b51880505d2"
HOST = "ec2-107-20-243-220.compute-1.amazonaws.com"
PORT = "5432"

CREDENTIALS = ("dbname=%s user=%s password=%s host=%s port=%s"
               % (DATABASE, USER, PASSWORD, HOST, PORT))

class GetFullName():
    def on_get(self, req, resp):
        try:
            email = str.strip(req.media.get("email"))
        except (KeyError, TypeError):
            resp.status = falcon.HTTP_400
            resp.media = {"message": "JSON Format Error"}
            return

        try:
            con = psycopg2.connect(CREDENTIALS)
            with con:
                cur = con.cursor()
                cur.execute("""
                            select full_name from user_info
                                where email=%s;""",
                            [email])
                name = cur.fetchone()[0]
                print(name)
        except psycopg2.OperationalError:
            resp.status = falcon.HTTP_503
            resp.media = {"message": "Connection terminated"}
            return
        except psycopg2.ProgrammingError:
            resp.status = falcon.HTTP_400
            resp.media = {"message": "User does not exist"}
            return

        resp.status = falcon.HTTP_200
        resp.media = {"name": name}

class ManageGroupEvents():
    def on_get(self, req, resp):
        try:
            length_of_event = datetime.datetime.strptime(str.strip(req.media.get("lengthOfEvent")), "%H:%M")
            length_of_event = datetime.timedelta(hours=length_of_event.hour, minutes=length_of_event.minute)

            group_name = str.strip(req.media.get("groupName"))
            creator = str.strip(req.media.get("owner"))
        except (KeyError, TypeError):
            resp.status = falcon.HTTP_400
            resp.media = {"message": "JSON Format Error"}
            return

        try:
            con = psycopg2.connect(CREDENTIALS)

            with con:
                cur = con.cursor()
                cur.execute("""
                            select members from groups
                                where group_name=%s and owner_email=%s;""",
                            [group_name, creator])
                users = cur.fetchone()[1].append(cur.fetchone()[0])
                
                events = []
                for user in users:
                    cur.execute("""
                                select start_time, end_time, start_date,
                                       end_date, days_of_week from events
                                    where owner_email=%s;""",
                                [user])
                    events.extend(cur.fetchall())
        except psycopg2.OperationalError:
            resp.status = falcon.HTTP_503
            resp.media = {"message": "Connection terminated"}

        keys = ["startTime", "endTime", "startDate", "endDate",
                "daysOfWeek"]
            
        events = [dict((key, value) for key, value in zip(keys, x))
                                    for x in events]

        mapped_events = []
        mapped_keys = ["timeSlot", "dateRange", "daysOfWeek"]
        event_builder = {key: None for key in mapped_keys}
        for x in events:
            event_builder["timeSlot"] = (x["startTime"], x["endTime"])
            event_builder["dateRange"] = (x["startDate"],
                          x["endDate"])
            event_builder["daysOfWeek"] = [time.strptime(y, "%A").tm_wday for y in x["daysOfWeek"]]
            mapped_events.append(event_builder)

        starting_times = []
        for day in [datetime.date.today() + datetime.timedelta(days=x) for x in range(14)]:
            today_events = []
            for event in mapped_events:
                if event["endDate"] == event["startDate"] == day:
                    today_events.append(event)
                elif (event["endDate"] >= datetime.date.today() and event["startDate"] <= datetime.data.today()
                      and day.weekday() in event["daysOfweek"]):
                    today_events.append(event)
            today_events = [x for x in today_events if x >= datetime.datetime.now()]
            today_events = [x["timeSlot"] for x in today_events]
                   
            condensed_times = []
            event = today_events[0]
            for x in today_events[1:]:
                if event[1] >= x[0]:
                    event = (min(event[0], x[0]), max(event[1], x[1]))
                else:
                    condensed_times.append(event)
                    event = x
            else:
                condensed_times.append(event)

            condensed_times = sorted(condensed_times, key = lambda x: x[0])
            condensed_times = [(x[0] - (x[0] - datetime.datetime.min) % datetime.timedelta(minutes=15),
                                x[1] + (datetime.datetime.min - x[1]) % datetime.timedelta(minutes=15))
                                for x in condensed_times]
            start = datetime.datetime.now() 
            start = start + (datetime.datetime.min - start) % datetime.timedelta(hours=1)
                    
            legal_times = []
            next_time_slot_index = 0
            next_time_slot = condensed_times[next_time_slot_index]
            while start.date() == day.date() and next_time_slot_index < len(condensed_times):
                i = 0
                while start + datetime.timedelta(minutes=15 * i) + length_of_event <= next_time_slot[0] and (start + datetime.timdelta(minutes=15 * i)).date() == day.date():
                    legal_times.append(start + datetime.timedelta(minutes=15 * i))
                    i += 1
                start = next_time_slot[1]
                next_time_slot_index += 1
                next_time_slot = condensed_times[next_time_slot_index]
            starting_times.extend([datetime.datetime.combine(day, x) for x in legal_times])
            
            resp.status = falcon.HTTP_200
            resp.media = {"validStartingTimes": starting_times}
            
    def on_post(self, req, resp):
        try:
            group_name = str.strip(req.media.get("groupName"))
            creator_email = str.strip(req.media.get("owner"))
            event_name = str.strip(req.media.get("eventName"))
            start_time = datetime.datetime.strptime(str.strip(req.media.get("startTime")), "%H:%M").time()
            end_time = datetime.datetime.strptime(str.strip(req.media.get("endTime")), "%H:%M").time()
            date = datetime.datetime.strptime(str.strip(req.media.get("date")), "%m/%d%%Y").date()
        except (KeyError, TypeError):
            resp.status = falcon.HTTP_400
            resp.media = {"message": "JSON Format Error"}
            return
        
        try:
            con = psycopg2.connect(CREDENTIALS)
            
            with con:
                cur = con.cursor()
                cur.execute("""
                            select owner_email, members from groups
                                where %s=group_name and %s=owner_email;""",
                            [group_name, creator_email])
                result = cur.fetchone()
                all_members = result[1].append(result[0])
                
                for x in all_members:
                    cur.execute("""
                                insert into events(owner_email, event_name,
                                                   start_time, end_time,
                                                   start_date, end_date,
                                                   days_of_week)
                                values(%s,%s,%s,%s,%s,%s,%s);""",
                                [x, event_name, start_time, end_time,
                                 date, date, []])
                con.commit()
        except psycopg2.OperationalError:
            resp.status = falcon.HTTP_503
            resp.media = {"message": "Connection terminated"}
            return
         
        resp.status = falcon.HTTP_200
        resp.media = {"message": "Added group event to all members"}

class CreateUser():
    def on_post(self, req, resp):
        try:
            email = str.strip(req.media.get("email"))
            full_name = str.strip(req.media.get("fullName"))
            password = str.strip(req.media.get("password"))
            confirm_password = str.strip(req.media.get("confirmPassword"))
        except (KeyError, TypeError):
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
        except (KeyError, TypeError):
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
        except (KeyError, TypeError):
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
            
        events = [dict((key, value) for key, value in zip(keys, x))
                                    for x in events]

        for x in events:
            x["startTime"] = x["startTime"].strftime("%H:%M")
            x["endTime"] = x["endTime"].strftime("%H:%M")
            x["startDate"] = x["startDate"].strftime("%m/%d/%Y")
            x["endDate"] = x["endDate"].strftime("%m/%d/%Y")
            x["daysOfWeek"] = x["daysOfWeek"].replace("{", "").replace("}", "").split(",")

        resp.status = falcon.HTTP_200
        resp.media = events

    def on_post(self, req, resp): # Will include edit and create
        try:
            action = str.strip(req.media.get("action"))
            user = str.strip(req.media.get("email"))

            if action in ("delete", "edit"):
                event_id = req.media.get("eventID")
            if action in ("edit", "create"):
                event_name = str.strip(req.media.get("eventName"))
                start_time = datetime.datetime.strptime(str.strip(req.media.get("startTime")), "%H:%M").time()
                end_time = datetime.datetime.strptime(str.strip(req.media.get("endTime")), "%H:%M").time()
                start_date = datetime.datetime.strptime(str.strip(req.media.get("startDate")), "%m/%d/%Y").date()
                end_date = datetime.datetime.strptime(str.strip(req.media.get("endDate")), "%m/%d/%Y").date()
                days_of_week = list(map(str.strip, req.media.get("daysOfWeek")))
            if action not in ("delete", "edit", "create"):
                raise KeyError("Not a valid action")
        except (TypeError, KeyError):
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

class ManageGroups():
    def on_get(self, req, resp):
        try:
            user = str.strip(req.media.get("email"))
        except (KeyError, TypeError):
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
                                    where %s=any(members) or %s=owner_email;""",
                                [user,user])
                    joined_groups = cur.fetchall()
                except psycopg2.ProgrammingError:
                    pass

                try:
                    cur.execute("""
                                select * from groups
                                    where %s = any(invites);""",
                                [user])
                    invitations = cur.fetchall()
                except psycopg2.ProgrammingError:
                    pass
        except psycopg2.OperationalError:
            resp.status = falcon.HTTP_503
            resp.media = {"message": "Connection terminated"}
            return

        keys = ["groupName", "owner", "members", "invited"]
            
        if joined_groups is not None:
            joined_groups = [dict((key, value) for key, value in zip(keys, x))
                                               for x in joined_groups]
        if invitations is not None:
            invitations = [dict((key, value) for key, value in zip(keys, x))
                                             for x in invitations]
        

        resp.status = falcon.HTTP_201
        resp.media = {"groups": joined_groups if joined_groups is not None else [],
                      "invites": invitations if invitations is not None else []}

    def on_post(self, req, resp):
        print(req)
        try:
            action = str.strip(req.media.get("action"))

            group_name = str.strip(req.media.get("groupName"))
            creator_email = str.strip(req.media.get("owner"))
            if action in ("create", "delete"):
                pass
            if action == "invite":
                users = [str.strip(x)
                         for x in req.media.get("users").split(",")]
                print(users)
            if action in ("join", "remove"):
                member_email = str.strip(req.media.get("email"))
        except (KeyError, AssertionError, TypeError):
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
                    con.commit()
                    return

                if action == "delete":
                    cur.execute("""
                                delete from groups
                                    where group_name = %s and owner_email = %s;
                                """, [group_name, creator_email])

                    resp.status = falcon.HTTP_200
                    resp.media = {"message": "Group deleted"}
                    con.commit()
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
                                        and (%s=any(invites) or
                                             %s=any(members)));""",
                                    [group_name, creator_email, x, x])
                        if cur.fetchone()[0]:
                            invalid_users.append(x)
                    users = [x for x in users if x not in invalid_users]

                    cur.execute("""
                                update groups
                                    set invites=array_cat(invites,%s::citext[])
                                    where group_name=%s and owner_email=%s;""",
                                [users, group_name, creator_email])

                    resp.status = falcon.HTTP_200
                    response = {"message": "Attempted inviting users",
                                "valid_invitations": users}
                    if len(invalid_users) != 0:
                        response["invalid_invitations"] = invalid_users
                    resp.media = response
                    con.commit()
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
                                    and %s=any(invites));""",
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
                                    set members=array_append(members,%s)
                                    where group_name=%s and owner_email=%s;""",
                                [member_email, group_name, creator_email])

                    resp.status = falcon.HTTP_200
                    resp.media = {"message": "Joined group successfully"}
                    con.commit()

                if action == "remove":
                    print("member_email: {} == creator_email: {}".format(
                          member_email, creator_email))
                    if member_email == creator_email:
                        resp.status = falcon.HTTP_400
                        resp.media = {"message": "Cannot remove owner"}
                        return

                    cur.execute("""
                                select exists(select * from groups
                                    where group_name=%s and owner_email=%s
                                    and (%s=any(invites) or %s=any(members)));""",
                                [group_name, creator_email, member_email,
                                 member_email])
                    if not cur.fetchone()[0]:
                        resp.status = falcon.HTTP_400
                        resp.media = {"message": "User not in group"}
                        return

                    cur.execute("""
                                update groups
                                    set invites=array_remove(invites,%s),
                                        members=array_remove(members,%s)
                                    where group_name=%s and owner_email=%s;""",
                                [member_email, member_email, group_name,
                                 creator_email])
                    con.commit()
                    resp.status = falcon.HTTP_200
                    resp.media = {"message": "User declined invite successfully"}
        except psycopg2.OperationalError:
            resp.status = falcon.HTTP_503
            resp.media = {"message": "Connection terminated"}
            return
        except psycopg2.ProgrammingError:
            resp.status = falcon.HTTP_400
            resp.media = {"message": "Group does not exist"}
            return
        except psycopg2.errors.UniqueViolation:
            resp.status = falcon.HTTP_400
            resp.media = {"message": "Group already exists"}

API = falcon.API()
CREATEUSER_ENDPOINT = CreateUser()
USERLOGIN_ENDPOINT = UserLogin()
MANAGEEVENTS_ENDPOINT = ManageEvents()
MANAGEGROUPS_ENDPOINT = ManageGroups()
GETFULLNAME_ENDPOINT = GetFullName()
MANAGEGROUPEVENTS_ENDPOINT = ManageGroupEvents()
API.add_route("/createUser", CREATEUSER_ENDPOINT)
API.add_route("/loginUser", USERLOGIN_ENDPOINT)
API.add_route("/manageEvents", MANAGEEVENTS_ENDPOINT)
API.add_route("/manageGroups", MANAGEGROUPS_ENDPOINT)
API.add_route("/getFullName", GETFULLNAME_ENDPOINT)
API.add_route("/manageGroupEvents", MANAGEGROUPEVENTS_ENDPOINT)