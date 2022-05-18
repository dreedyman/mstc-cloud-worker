IMAGE_NAME = mstc/python-test:latest

dist:
	docker build . -t $(IMAGE_NAME)

run: dist
	docker run --rm $(IMAGE_NAME)

clean:
	docker rmi $(IMAGE_NAME) -f
	
.PHONY: clean
	