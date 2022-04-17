import pytest

from mstc_cloud_worker import Client
from mstc_cloud_worker import Data

"""
def test_send(config, inputs, client, data, cleanup):    
    items = data.upload(config['minio']['bucket'], inputs)
    assert items is not None
    assert len(items) == 2
    send_data = {"endpoint": data.endpoint,
                 "bucket": config['minio']['bucket'],
                 "itemNames": items,
                 "image": "mstc/astros-eap-12.5:0.2.0",
                 "jobName": "astros",
                 "args":["echo","Running a job"],
    }
    # client.send(send_data)
"""

def test_send_http(inputs, data, config):
    import requests
    items = ["foo", "bar"]
    send_data = {"endpoint": data.endpoint,
                 "bucket": config['minio']['bucket'],
                 "itemNames": items,
                 "image": "busybox",
                 "jobName": "say so",
                 "command":["/bin/sh"],
                 "args":["-c", "while true; do echo hello; sleep 10;done"],
    }
    r = requests.post('http://localhost:8080/mstc/worker/execute', json=send_data)
    print(r.status_code)
    print(r.json())

    