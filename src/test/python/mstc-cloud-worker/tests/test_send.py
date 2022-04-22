import pytest
from pathlib import Path
import os
#from mstc_cloud_worker import Client
#from mstc_cloud_worker import Data


#def test_send(config, inputs, client, data, cleanup):    

def test_send(config, client, data, inputs):
    items = data.upload("test.inputs", inputs)
    assert items is not None
    assert len(items) == 2
    bucket = config['minio']['bucket']
    send_data = {"image": "mstc/python-test",
                 "jobName": "test-job",
                 "inputBucket" : bucket
                 }    
    result = client.send(send_data)
    content = result["result"]
    for line in content.split("\n"):
        print(line)
    
    path = Path(os.path.dirname(__file__))
    files_dir = os.path.join(path, "scratch")
        
    downloaded = data.download("test.inputs", files_dir)
    assert len(downloaded) == 2
    
"""

def test_send_http(data, config):
    import requests
    bucket = config['minio']['bucket']
    send_data = {"image": "mstc/python-test",
                 "jobName": "test-job",
                 #"timeOut": 5,
                 "inputBucketUrl" : data.endpoint + "/" + bucket
                 }
    r = requests.post('http://localhost:8080/mstc/worker/execute', json=send_data)
    print(r.status_code)
    #print(r.json())
    print(str(r))
    print(str(r.content))

"""    