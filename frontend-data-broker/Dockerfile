FROM python:3.13-slim
WORKDIR /frontend-code

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .
CMD ["gunicorn", "-b", "0.0.0.0:5000", "mysql_api:app"]
