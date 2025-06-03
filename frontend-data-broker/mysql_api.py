import os
from flask import Flask, jsonify
import mysql.connector

app = Flask(__name__)

@app.route('/users')
def get_users():
    return query("SELECT * FROM users")

@app.route('/maps')
def get_maps():
    return query("SELECT * FROM maps")

@app.route('/scores')
def get_scores():
    return query("SELECT * FROM scores")

def query(sql):
    conn = mysql.connector.connect(
        host=os.getenv('DB_HOST', 'db'),
        user=os.getenv('DB_USER', 'smcb'),
        password=os.getenv('DB_PASSWORD', ''),
        database=os.getenv('DB_NAME', 'mowc')
    )
    cursor = conn.cursor()
    cursor.execute(sql)
    rows = cursor.fetchall()
    cursor.close()
    conn.close()
    return jsonify(rows)

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
