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
        host='127.0.0.1',
        user='smcb',
        password='',
        database='mowc'
    )
    cursor = conn.cursor()
    cursor.execute(sql)
    rows = cursor.fetchall()
    cursor.close()
    conn.close()
    return jsonify(rows)

