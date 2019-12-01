from contextlib import closing
import re
import datetime

import falcon
import psycopg2
import argon2

DATABASE = "d814roat3puk53"
USER = "evsifgooyevaft"
PASSWORD = "80f763bb1196c19be42f375323dedbfd6080cdec0605f12a689c5b51880505d2"
HOST = "ec2-107-20-243-220.compute-1.amazonaws.com"
PORT = "5432"

CREDENTIALS = ("dbname=%s user=%s password=%s host=%s port=%s"
               % (DATABASE, USER, PASSWORD, HOST, PORT))


class CreateUser:
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
            with closing(psycopg2.connect(CREDENTIALS)) as con:
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

        def hasher(x): return argon2.PasswordHasher().hash(x)
        user_hash = hasher(form_validation.get("result"))

        entry_creation = CreateUser.create_user_entry(form.get("full_name"),
                                                      form.get("email"),
                                                      user_hash)

        resp.status = entry_creation.get("result")[0]
        resp.media = {"message": entry_creation.get("result")[1]}


class GetFullName:
    @staticmethod
    def form_parser(form):
        is_valid = True
        try:
            email = str.strip(form.get("email"))
        except (KeyError, TypeError):
            is_valid = False
            result = "JSON Format Error"
        else:
            result = {"email": email}

        return {"is_valid": is_valid, "result": result}

    @staticmethod
    def fetch_user_name(email):
        is_success = False
        try:
            with closing(psycopg2.connect(CREDENTIALS)) as con:
                with con, con.cursor() as cur:
                    cur.execute("""
                                select full_name from user_info
                                    where email = %s;""",
                                [email])
                    name = cur.fetchone()[0]
        except psycopg2.OperationalError:
            result = falcon.HTTP_503, "Generic database error"
        except TypeError: # None type return on no matches
            result = falcon.HTTP_406, "User does not exist"
        else:
            is_success = True
            result = falcon.HTTP_200, name

        return {"is_success": is_success, "result": result}

    def on_get(self, req, resp):
        form = GetFullName.form_parser(req.media)
        if not form.get("is_valid"):
            resp.status = falcon.HTTP_406
            resp.media = {"message": form.get("result")}
            return

        email = form.get("result").get("email")
        fullname_retrieval = GetFullName.fetch_user_name(email)

        resp.status = fullname_retrieval.get("result")[0]
        if not fullname_retrieval.get("is_success"):
            resp.media = {"message": fullname_retrieval.get("result")[1]}
        else:
            resp.media = {"name": fullname_retrieval.get("result")[1]}


class UserLogin:
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
            with closing(psycopg2.connect(CREDENTIALS)) as con:
                with con, con.cursor() as cur:
                    cur.execute("""
                                select hash from user_info
                                    where email=%s;""",
                                [user])
                    user_hash = cur.fetchone()[0]
        except psycopg2.OperationalError:
            result = falcon.HTTP_503, "Generic database error"
        except TypeError: # None type return on no match
            result = falcon.HTTP_406, "User does not exist"
        else:
            is_success = True
            result = user_hash

        return {"is_success": is_success, "result": result}

    @staticmethod
    def verify_identity(password, user_hash):
        try:
            argon2.PasswordHasher().verify(user_hash, password)
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
            event_builder["daysOfWeek"] = [datetime.datetime.strptime(y, "%A").tm_wday for y in x["daysOfWeek"]]
            mapped_events.append(event_builder)

        starting_times = []
        for day in [datetime.date.today() + datetime.timedelta(days=x) for x in range(14)]:
            today_events = []
            for event in mapped_events:
                if event["endDate"] == event["startDate"] == day:
                    today_events.append(event)
                elif (event["endDate"] >= datetime.date.today() and event["startDate"] <= datetime.date.today()
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

            condensed_times = sorted(condensed_times, key=lambda x: x[0])
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
                while start + datetime.timedelta(minutes=15 * i) + length_of_event <= next_time_slot[0] and (start + datetime.timedelta(minutes=15 * i)).date() == day.date():
                    legal_times.append(start + datetime.timedelta(minutes=15 * i))
                    i += 1
                start = next_time_slot[1]
                next_time_slot_index += 1
                next_time_slot = condensed_times[next_time_slot_index]
            starting_times.extend([datetime.datetime.combine(day, x) for x in legal_times])

            resp.status = falcon.HTTP_200
            resp.media = {"validStartingTimes": starting_times}
            return

    def on_post(self, req, resp):
        try:
            group_name = str.strip(req.media.get("groupName"))
            creator_email = str.strip(req.media.get("owner"))
            event_name = str.strip(req.media.get("eventName"))
            start_time = datetime.datetime.strptime(str.strip(req.media.get("startTime")), "%H:%M").time()
            end_time = datetime.datetime.strptime(str.strip(req.media.get("endTime")), "%H:%M").time()
            date = datetime.datetime.strptime(str.strip(req.media.get("date")), "%m/%d/%Y").date()
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
                all_members = result[1].replace("{", "").replace("}", "").split(",")
                all_members.append(result[0])

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
        return


class ManageEvents:
    @staticmethod
    def get_functions(function, *args):
        def form_parse(form):
            is_valid = True
            try:
                user = str.strip(form.get("email"))
            except (KeyError, TypeError):
                is_valid = False
                result = "JSON Form Error"
            else:
                result = {"user": user}

            return {"is_valid": is_valid, "result": result}

        def get_user_events(user):
            is_success = True
            try:
                with closing(psycopg2.connect(CREDENTIALS)) as con:
                    with con, con.cursor() as cur:
                        cur.execute("""
                                    select * from events
                                        where owner_email = %s""",
                                    [user])
                        result = cur.fetchall()
            except psycopg2.OperationalError:
                is_success = False
                result = falcon.HTTP_503, "Generic database error"

            return {"is_success": is_success, "result": result}

        function_map = {"form_parse": form_parse,
                        "get_user_events": get_user_events}
        return function_map.get(function)(args)

    def on_get(self, req, resp): # Ask for all events of a user
        form = ManageEvents.get_functions("form_parse", req.media)
        if not form.get("is_valid"):
            resp.status = falcon.HTTP_406
            resp.media = {"message": form.get("result")}
            return

        user = form.get("result").get("user")
        events = ManageEvents.get_functions("get_user_events", user)
        if not events.get("is_success"):
            resp.status = events.get("result")[0]
            resp.media = {"message": events.get("result")[1]}
            return
        events = events.get("result")

        keys = ["eventID", "email", "eventName", "startTime", "endTime",
                "startDate", "endDate", "daysOfWeek"]

        events = [dict((key, value) for key, value in zip(keys, x))
                                    for x in events]

        for x in events:
            x["startTime"] = x["startTime"].strftime("%H:%M")
            x["endTime"] = x["endTime"].strftime("%H:%M")
            x["startDate"] = x["startDate"].strftime("%m/%d/%Y")
            x["endDate"] = x["endDate"].strftime("%m/%d/%Y")
            x["daysOfWeek"] = list(filter(None, x["daysOfWeek"]. \
                                                replace("{", ""). \
                                                replace("}", "").split(",")))

        resp.status = falcon.HTTP_200
        resp.media = events

    @staticmethod
    def post_functions(function, *args):
        def form_parse(form):
            is_valid = True
            result = {}
            try:
                result["action"] = str.strip(form.get("action"))
                if result.get("action") not in ("create", "edit", "delete"):
                    raise KeyError

                result["user"] = str.strip(form.get("email"))

                if result.get("action") in ("delete", "edit"):
                    result["event_id"] = str.strip(form.get("eventID"))
                if result.get("action") in ("edit", "create"):
                    result["event_name"] = str.strip(form.get("eventName"))
                    result["start_time"] = datetime.datetime.strptime(
                                               str.strip(form.get("startTime")),
                                               "%H:%M"
                                           ).time()
                    result["end_time"] = datetime.datetime.strptime(
                                             str.strip(form.get("endtime")),
                                             "%H:%M"
                                         ).time()
                    result["start_date"] = datetime.datetime.strptime(
                                               str.strip(form.get("startDate")),
                                               "%m/%d/%Y"
                                           ).time()
                    result["end_date"] = datetime.datetime.strptime(
                                               str.strip(form.get("endDate")),
                                               "%m/%d/%Y"
                                           ).time()
                    result["days_of_week"] = [x for x
                                              in form.get("daysOfWeek").\
                                                  split(",")
                                              if x != ""]
            except (TypeError, KeyError):
                is_valid = False
                result = "JSON Form Error"

            return {"is_valid": is_valid, "result": result}

        def create_personal_event(owner_email, event_name, start_time, end_time,
                                  start_date, end_date, days_of_week):
            is_success = False
            try:
                with closing(psycopg2.connect(CREDENTIALS)) as con:
                    with con, con.cursor() as cur:
                        cur.execute("""
                                    insert into events(owner_email, event_name,
                                                       start_time, end_time,
                                                       start_date, end_date,
                                                       days_of_week)
                                        values(%s,%s,%s,%s,%s,%s,%s);""",
                                    [owner_email, event_name, start_time,
                                     end_time, start_date, end_date,
                                     days_of_week])
            except psycopg2.OperationalError:
                result = falcon.HTTP_503, "Generic database error"
            else:
                is_success = True
                result = falcon.HTTP_201, "Event created successfully"

            return {"is_success": is_success, "result": result}

        def edit_personal_event(event_name, start_time, end_time, start_date,
                                end_date, days_of_week, event_id, owner_email):
            is_success = False
            try:
                with closing(psycopg2.connect(CREDENTIALS)) as con:
                    with con, con.cursor() as cur:
                        cur.execute("""
                                    update events set event_name=%s,
                                                      start_time=%s,
                                                      end_time=%s,
                                                      start_date=%s,
                                                      end_date=%s,
                                                      days_of_week=%s,
                                        where event_id=%s and owner_email=%s;
                                    """,
                                    [event_name, start_time, end_time,
                                     start_date, end_date, days_of_week,
                                     event_id, owner_email])
            except psycopg2.OperationalError:
                result = falcon.HTTP_503, "Generic database error"
            else:
                is_success = True
                result = falcon.HTTP_200, "Event edited successfully"

            return {"is_success": is_success, "result": result}

        def delete_personal_event(event_id, owner_email):
            is_success = False
            try:
                with closing(psycopg2.connect(CREDENTIALS)) as con:
                    with con, con.cursor() as cur:
                        cur.execute("""
                                    delete from events
                                        where event_id=%s and owner_email=%s;
                                    """, [event_id, owner_email])
            except psycopg2.OperationalError:
                result = falcon.HTTP_503, "Generic database erorr"
            else:
                is_success = True
                result = falcon.HTTP_200, "Event deleted successfully"

            return {"is_success": is_success, "result": result}

        function_map = {"form_parse": form_parse,
                        "create_personal_event": create_personal_event,
                        "edit_personal_event": edit_personal_event,
                        "delete_personal_event": delete_personal_event}

        return function_map.get(function)(args)


    def on_post(self, req, resp): # Will include edit and create
        form = ManageEvents.post_functions("form_parse", req.media)
        if not form.get("is_valid"):
            resp.status = falcon.HTTP_406
            resp.media = {"message": form.get("result")[1]}
            return
        form = form.get("result")

        if form.get("action") == "create":
            result = ManageEvents.post_functions("create_personal_event",
                                                 form.get("user"),
                                                 form.get("event_name"),
                                                 form.get("start_time"),
                                                 form.get("end_time"),
                                                 form.get("start_date"),
                                                 form.get("end_date"),
                                                 form.get("days_of_week"))
        elif form.get("action") == "edit":
            result = ManageEvents.post_functions("edit_personal_event",
                                                  form.get("event_name"),
                                                  form.get("start_time"),
                                                  form.get("end_time"),
                                                  form.get("start_date"),
                                                  form.get("end_date"),
                                                  form.get("days_of_week"),
                                                  form.get("event_id"),
                                                  form.get("user"))
        elif form.get("action") == "delete":
            result = ManageEvents.post_functions("delete_personal_events",
                                                 form.get("event_id"),
                                                 form.geT("user"))

        resp.status = result.get("result")[0]
        resp.media = {"message": result.get("result")[1]}

class ManageGroups:
    @staticmethod
    def get_functions(function, *args):
        def form_parse(form):
            is_valid = True
            try:
                email = str.strip(form.get("email"))
            except (KeyError, TypeError):
                is_valid = False
                result = "JSON Form Error"
            else:
                result = {"email": email}

            return {"is_valid": is_valid, "result": result}

        def get_membership(user):
            is_success = True
            try:
                with closing(psycopg2.connect(CREDENTIALS)) as con:
                    with con, con.cursor() as cur:
                        cur.execute("""
                                    select * from groups
                                        where any(members) = %s or
                                              owner_email = %s;""",
                                    [user, user])
                        joined_groups = cur.fetchall()

                        cur.execute("""
                                    select * from groups
                                        where any(invites) = %s;""",
                                    [user])
                        invitations = cur.fetchall()
            except psycopg2.OperationalError:
                is_success = False
                result = falcon.HTTP_503, "Generic database error"
            else:
                result = {"joined_groups": joined_groups,
                          "invitations": invitations}

            return {"is_success": is_success, "result": result}

        def organize_groups(keys, joined_groups, invitations):
            joined_groups = [dict((key, value) for key, value in zip(keys, x))
                                               for x in joined_groups]

            invitations = [dict((key, value) for key, value in zip(keys, x))
                                             for x in invitations]

            return {"joined_groups": joined_groups, "invitations": invitations}

        function_map = {"form_parse": form_parse,
                        "get_membership": get_membership,
                        "organize_groups": organize_groups}

        return function_map.get(function)(*args)

    def on_get(self, req, resp):
        form = ManageGroups.get_functions("form_parse", req.media)
        if not form.get("is_valid"):
            resp.status = falcon.HTTP_406
            resp.media = {"message": form.get("result")}
            return

        email = form.get("result").get("email")
        membership = ManageGroups.get_functions("get_membership", email)
        if not membership.get("is_success"):
            resp.status = membership.get("result")[0]
            resp.media = {"message": membership.get("result")[1]}
            return

        joined_groups = membership.get("result").get("joined_groups")
        invitations = membership.get("result").get("invitations")
        keys = ["groupName", "owner", "members", "invited"]

        organized_groups = ManageGroups.get_functions("organize_data", keys,
                                                      joined_groups,
                                                      invitations)

        resp.status = falcon.HTTP_201
        resp.media = {"groups": organized_groups.get("joined_groups"),
                      "invites": organized_groups.get("invitations")}

    @staticmethod
    def post_functions(function, *args):
        def form_parse(form):
            is_valid = True
            try:
                result = {}
                result["action"] = str.strip(form.get("action"))
                result["group_name"] =  str.strip(form.get("groupName"))
                result["creator_email"] = str.strip(form.get("owner"))

                if result.get("action") == "invite":
                    result["users"] = [str.strip(x)
                                       for x in form.get("users").split(",")]
                elif result.get("action") in ("join", "remove"):
                    result["member_email"] = str.strip(form.get("email"))

                if result.get("action") not in ("create", "invite", "join",
                                                "remove", "delete"):
                    raise KeyError
            except (KeyError, TypeError):
                is_valid = False
                result = "JSON Form Error"

            return {"is_valid": is_valid, "result": result}

        def create_group(group_name, creator_email):
            is_success = False
            try:
                with closing(psycopg2.connect(CREDENTIALS)) as con:
                    with con, con.cursor() as cur:
                        cur.execute("""
                                    select exists(select * from groups
                                        where group_name=%s and owner_email=%s)
                                    """, [group_name, creator_email])
                        if cur.fetchone()[0]:
                            raise psycopg2.errors.UniqueViolation

                        cur.execute("""
                                    insert into groups(group_name, owner_email,
                                                       members, invites)
                                        values(%s, %s, %s, %s);""",
                                    [group_name, creator_email, [], []])
            except psycopg2.OperationalError:
                result = falcon.HTTP_503, "Generic database error"
            except psycopg2.errors.UniqueViolation:
                result = falcon.HTTP_406, "Group already exists"
            else:
                is_success = True
                result = falcon.HTTP_201, "Group created successfully"

            return {"is_success": is_success, "result": result}

        def delete_group(group_name, owner_email):
            is_success = False
            try:
                with closing(psycopg2.connect(CREDENTIALS)) as con:
                    with con, con.cursor() as cur:
                        cur.execute("""
                                    delete from groups
                                        where group_name=%s and owner_email=%s;
                                    """, [group_name, owner_email])
            except psycopg2.OperationalError:
                result = falcon.HTTP_503, "Generic database error"
            else:
                is_success = True
                result = falcon.HTTP_200, "Group deleted successfully"

            return {"is_success": is_success, "result": result}

        def invite_to_group(group_name, owner_email, users):
            is_success = False
            try:
                invalid_users = []
                with closing(psycopg2.connect(CREDENTIALS)) as con:
                    with con, con.cursor() as cur:
                        for user in users:
                            cur.execute("""
                                        select exists(select * from user_info
                                            where email=%s);""",
                                        [user])
                            if not cur.fetchone()[0]:
                                invalid_users.append(user)

                            cur.execute("""
                                        select exists(select * from groups
                                            where group_name=%s and
                                            owner_email=%s and
                                            (%s=any(invites) or
                                             %s=any(members)));""",
                                        [group_name, owner_email,
                                         user, user])
                            if not cur.fetchone()[0]:
                                invalid_users.append(user)

                        users = [user for user in users
                                 if user not in invalid_users]

                        cur.execute("""
                                    update groups
                                        set invites=array_cat(invites,
                                                              %s::citext)
                                        where group_name=%s and owner_email=%s;
                                    """, [users, group_name, owner_email])
            except psycopg2.OperationalError:
                result = falcon.HTTP_503, "Generic database error"
            else:
                is_success = True
                result = {"valid_invitations": users,
                          "invalid_invitations": invalid_users}
                result = falcon.HTTP_200, result

            return {"is_success": is_success, "result": result}

        def join_group(group_name, owner_email, member_email):
            is_success = False
            try:
                with closing(psycopg2.connect(CREDENTIALS)) as con:
                    with con, con.cursor() as cur:
                        cur.execute("""
                                    update groups
                                        set invites=array_remove(invites,%s),
                                            members=array_append(members,%s)
                                        where group_name=%s and owner_email=%s
                                            and %s=any(invites);""",
                                    [member_email, member_email, group_name,
                                     owner_email ,member_email])
            except psycopg2.OperationalError:
                result = falcon.HTTP_503, "Generic database error"
            else:
                is_success = True
                result = falcon.HTTP_200, "Joined group successfully"

            return {"is_success": is_success, "result": result}

        def remove_from_group(group_name, owner_email, member_email):
            is_success = False
            try:
                if owner_email == member_email:
                    raise ValueError
                with closing(psycopg2.connect(CREDENTIALS)) as con:
                    with con, con.cursor() as cur:
                        cur.execute("""
                                    update groups
                                        set invites=array_remove(invites,%s),
                                            members=array_remove(members,%s)
                                        where group_name=%s and owner_email=%s;
                                    """, [member_email, member_email,
                                          group_name, owner_email])
            except psycopg2.OperationalError:
                result = falcon.HTTP_503, "Generic database error"
            except ValueError:
                result = falcon.HTTP_406, "Cannot remove owner"
            else:
                is_success = True
                result = falcon.HTTP_200, "User removed from group successfully"

            return {"is_success": is_success, "result": result}

        function_map = {"form_parse": form_parse,
                        "create_group": create_group,
                        "delete_group": delete_group,
                        "invite_to_group": invite_to_group,
                        "join_group": join_group,
                        "remove_from_group": remove_from_group}
        return function_map.get(function)(args)

    def on_post(self, req, resp):
        form = ManageGroups.post_functions("form_parse", req.media)
        if not form.get("is_valid"):
            resp.status = falcon.HTTP_406
            resp.media = {"message": form.get("result")}
            return

        form = form.get("result")
        action = form.get("action")

        if action == "create":
            result = ManageGroups.post_functions("create_group",
                                                 form.get("group_name"),
                                                 form.get("owner_email"))
        elif action == "delete":
            result = ManageGroups.post_functions("delete_group",
                                                 form.get("group_name"),
                                                 form.get("owner_email"))
        elif action == "invite":
            result = ManageGroups.post_functions("invite_to_group",
                                                 form.get("group_name"),
                                                 form.get("owner_email"),
                                                 form.get("members"))
        elif action == "join":
            result = ManageGroups.post_functions("join_group",
                                                 form.get("group_name"),
                                                 form.get("owner_email"),
                                                 form.get("member_email"))
        elif action == "remove":
            result = ManageGroups.post_functions("remove_from_group",
                                                 form.get("group_name"),
                                                 form.get("owner_email"),
                                                 form.get("member_email"))
        resp.status = result.get("result")[0]
        resp.media = result.get("result")[1]
        if action == "invite" and result.get("is_success"):
            resp.media["message", "Attempted inviting users"]


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
