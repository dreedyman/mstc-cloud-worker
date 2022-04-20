import pytest

#from mstc_cloud_worker import Client
#from mstc_cloud_worker import Data


def test_send(config, inputs, client, data, cleanup):    
    #items = data.upload(config['minio']['bucket'], inputs)
    #assert items is not None
    #assert len(items) == 2
    bucket = config['minio']['bucket']
    send_data = {"image": "mstc/python-test",
                 "jobName": "test-job",
                 "inputBucketUrl" : data.endpoint + "/" + bucket
                 }    
    client.send(send_data)
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