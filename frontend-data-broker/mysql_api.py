import os, logging, requests
import mysql.connector
from flask import Flask, jsonify, g, request as flask_request, redirect, render_template

app = Flask(__name__)
logging.basicConfig(level=logging.INFO)

# Environment Variables
app.config['DB_HOST'] = os.getenv('DB_HOST', 'db')
app.config['DB_USER'] = os.getenv('DB_USER', 'smcb')
app.config['DB_PASSWORD'] = os.getenv('DB_PASSWORD', '')
app.config['DB_NAME'] = os.getenv('DB_NAME', 'mowc')
app.config['OSU_CLIENT_ID'] = os.getenv('OSU_CLIENT_ID')
app.config['OSU_CLIENT_SECRET'] = os.getenv('OSU_CLIENT_SECRET')
app.config['REDIRECT_URI'] = os.getenv('REDIRECT_URI')
app.config['DISCORD_RETURN_URL'] = os.getenv('DISCORD_RETURN_URL')
app.config['DISCORD_RETURN_NAME'] = os.getenv('DISCORD_RETURN_NAME')


# Connect to DB
def get_db():
    if 'db' not in g:
        g.db = mysql.connector.connect(
            host=app.config['DB_HOST'],
            user=app.config['DB_USER'],
            password=app.config['DB_PASSWORD'],
            database=app.config['DB_NAME']
        )
    return g.db


@app.teardown_appcontext
def close_db(error):
    db = g.pop('db', None)
    if db:
        db.close()


def run_query(sql, params=None):
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute(sql, params or ())
    result = cursor.fetchall()
    cursor.close()
    return result


def run_update(sql, params=None):
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute(sql, params or ())
    conn.commit()
    cursor.close()



# Routes
@app.route('/users')
def get_users():
    return jsonify(run_query("SELECT * FROM users"))


@app.route('/maps')
def get_maps():
    return jsonify(run_query("SELECT * FROM maps"))


@app.route('/scores')
def get_scores():
    return jsonify(run_query("SELECT * FROM scores"))


@app.route('/osu-callback')
def osu_callback():
    code = flask_request.args.get("code")
    discord_id = flask_request.args.get("state")

    # Get token from code.
    response = requests.post("https://osu.ppy.sh/oauth/token", json={
        "client_id": app.config['OSU_CLIENT_ID'],
        "client_secret": app.config['OSU_CLIENT_SECRET'],
        "code": code,
        "grant_type": "authorization_code",
        "redirect_uri": app.config['REDIRECT_URI']
    })

    logging.info("Raw token response: %s %s", response.status_code, response.text)

    try:
        token_response = response.json()
    except Exception as e:
        logging.error("Failed to parse JSON: %s", e)
        return "Invalid response from osu!", 500

    if "access_token" not in token_response:
        logging.error("Token error: %s", token_response)
        return "Failed to get access token", 400

    access_token = token_response["access_token"]

    # Get user info.
    user_response = requests.get("https://osu.ppy.sh/api/v2/me", headers={
        "Authorization": f"Bearer {access_token}"
    }).json()

    user_id = user_response["id"]
    username = user_response["username"]
    country_code = user_response["country"]["code"]

    # Insert or update user info
    run_update("""
        INSERT INTO users (user_id, username, country_code, verified, discord_id)
        VALUES (%s, %s, %s, %s, %s)
        ON DUPLICATE KEY UPDATE
            username = VALUES(username),
            country_code = VALUES(country_code),
            verified = VALUES(verified),
            discord_id = VALUES(discord_id)
    """,
    (user_id, username, country_code, 0, discord_id))

    return redirect('/success')


@app.route('/success')
def success():
    return render_template(
            "success.html",
            discord_return_url=app.config['DISCORD_RETURN_URL'],
            discord_return_name=app.config['DISCORD_RETURN_NAME']
    )



if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
