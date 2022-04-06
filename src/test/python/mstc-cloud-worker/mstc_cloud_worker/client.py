import pika  
import json  
import uuid

class Client:
    def __init__(self, host, exchange, work_queue):
        self._connection = pika.BlockingConnection(pika.ConnectionParameters(host=host))
        self._channel = self._connection.channel()
        self._exchange = exchange
        self._work_queue = work_queue

        # Declare work queue
        self._channel.queue_declare(queue=work_queue, durable=True)
    
        result = self._channel.queue_declare(queue='', exclusive=True)
        self._callback_queue = result.method.queue
        self._channel.basic_consume(
                    queue=self._callback_queue,
                    on_message_callback=self.on_response,
                    auto_ack=True)

    def on_response(self, ch, method, props, body):
        if self._corr_id == props.correlation_id:
            self.response = body

    def send(self, data):
        self.response = None
        #data = { "image":"mstc/astros-eap-12.5:0.2.0",
        #         "inputs":["s3://tenbar/input1.txt","s3://tenbar/input2.txt"],
        #         "createdTimestamp":"2022-03-29T18:06:53.477Z"
        #}
        message = json.dumps(data)
        self._corr_id = str(uuid.uuid4())
        # Send data
        self._channel.basic_publish(exchange=self._exchange, 
                                    routing_key=self._work_queue, 
                                    properties=pika.BasicProperties(
                                        reply_to=self._callback_queue,
                                        correlation_id=self._corr_id,
                                        ),
                                    body=message)
        print("[x] Sent data to RabbitMQ")
        print("[x] Wait for response", end='')
        while self.response is None:
            print(".", end='')
            self._connection.process_data_events()
        print("")
        result = json.loads(self.response)
        print("===> " + str(result))
        
        
    def close(self):    
        self._connection.close()
        
        
