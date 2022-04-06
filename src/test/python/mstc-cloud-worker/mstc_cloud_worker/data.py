from minio import Minio

class Data:
    def __init__(self, host: str, port: int, username: str, password: str):
        self._host = host
        self._port = port
        self._client = Minio(endpoint = f"{host}:{port}",
            access_key=username,
            secret_key=password,
            secure=False,)
        
    def bucket(self, bucket):
        found = self._client.bucket_exists(bucket)
        if not found:
            self._client.make_bucket(bucket)
            
    @property
    def client(self):
        return self._client        
    
    @property
    def endpoint(self):
        return f"{self._host}:{self._port}"
        
    def upload(self, bucket, files):
        import os
        self.bucket(bucket)        
        #urls = []
        items = []
        for file in files:
            name = os.path.basename(file)
            self.client.fput_object(bucket, name, file)
            #url = self._client.presigned_get_object(bucket, name)
            #urls.append(url)
            item = {"endpoint": f"{self._host}:{self._port}", "bucket": bucket, "itemName": name}
            items.append(item)
        return items