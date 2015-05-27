# URL Shortener

A simple URL shortener implementation to demonstrate the [Play Framework](https://www.playframework.com/).

## Running

This project should be runnable with `sbt run`

## API

To shorten a URL:

    curl localhost:9000/urls -X POST --data '{ "url": "https://google.com" }' -H 'Content-Type: application/json'

To hit a shortened URL:

    curl localhost:9000/b -L