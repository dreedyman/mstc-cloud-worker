# Base image
FROM python:3.9.0

# Install tools
RUN apt-get update && apt-get install -y vim dnsutils net-tools

ENV PIP_DISABLE_PIP_VERSION_CHECK=on
RUN pip install poetry

WORKDIR /app
COPY poetry.lock pyproject.toml /app/

RUN poetry config virtualenvs.create false
RUN poetry install --no-interaction

COPY ./mstc_cloud_worker/main.py /app/main.py
COPY ./mstc_cloud_worker/data.py /app/data.py
COPY ./mstc_cloud_worker/__init__.py /app/__init__.py

#ENV PYTHONPATH /app

#CMD ["/app/bin/startup.sh"]
ENTRYPOINT python /app/main.py $@
