import pytest

from mstc_cloud_worker import Client
from mstc_cloud_worker import Data

def test_send(config, inputs, client, data, cleanup):    
    items = data.upload(config['minio']['bucket'], inputs)
    assert items is not None
    assert len(items) == 2
    data = { "image": "mstc/astros-eap-12.5:0.2.0",
            "jobName": "astros",
             "inputs": items,
    }
    client.send(data)