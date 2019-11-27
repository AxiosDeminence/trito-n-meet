from contextlib import closing

import psycopg2
import falcon


class GetFullName:
    _CREDENTIALS = None

    def __init__(self, credentials):
        GetFullName._CREDENTIALS = credentials

    @staticmethod
    def form_parser(form):
        is_valid = True
        try:
            email = str.strip(form.get("email"))
        except (KeyError, TypeError):
            is_valid = False
            result = {"JSON Format Error"}
        else:
            result = {"email": email}

        return {"is_valid": is_valid, "result": result}

    @staticmethod
    def fetch_user_name(email):
        is_success = False
        try:
            with closing(psycopg2.connect(GetFullName._CREDENTIALS)) as con:
                with con, con.cursor() as cur:
                    cur.execute("""
                                select full_name from user_info
                                    where email=%s;""",
                                [email])
                    name = cur.fetchone()[0]
        except psycopg2.OperationalError:
            result = falcon.HTTP_503, "Generic database error"
        except psycopg2.ProgrammingError:
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

        email = form.get("result")
        fullname_retrieval = GetFullName.fetch_user_name(email)

        resp.status = fullname_retrieval.get("result")[0]
        if not fullname_retrieval.get("is_success"):
            resp.media = {"message": fullname_retrieval.get("result")[1]}
        else:
            resp.media = {"name": fullname_retrieval.get("result")[1]}
