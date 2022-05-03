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
        return f"http://{self._host}:{self._port}"
        
        
    def download(self, bucket, to):
        import os
        
        if not os.path.exists(to):
            os.makedirs(to)
            print("Created " + to)
        items = []
        for item in self._client.list_objects(bucket,recursive=True):
            name = item.object_name
            file_name = os.path.join(to, name)
            self._client.fget_object(bucket, item.object_name, file_name)
            items.append(file_name)
        return items        
        
        
    def upload(self, bucket, files):
        import os
        self.bucket(bucket)        
        #urls = []
        items = []
        for file in files:
            name = os.path.basename(file)
            self.client.fput_object(bucket, name, file)
            items.append(name)
        return items
    
    
    def delete(self, bucket):
        for item in self._client.list_objects(bucket,recursive=True):
            name = item.object_name
            self._client.remove_object(bucket, item.object_name)