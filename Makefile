run:
	./gradlew run

db:
	docker-compose up -d db

up:
	docker-compose up -d db && docker-compose up app

docker-run:
	docker run --rm -p 8080:8080 quay.io/wire/poll-bot

docker-build:
	docker build -t quay.io/wire/poll-bot:latest .

publish: docker-build
	docker push quay.io/wire/poll-bot:latest
