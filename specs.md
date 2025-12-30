# Specifications

I want to create a demo application to showcase the capabilities of Spark and Flink.

The source data will be generated. Data will be similar to a stock ticker. Each data point will be a stock symbol, event timestamp and the price of that stock at that time.

What I want to do using the stream processing is to send an alert if the stock price is 5% higher or lower than previous value.

To do this, I want to create three apps :

- Data generator (in the generator-app folder)
- Spark processor (in the spark-app folder)
- Flink processor (in the flink-app folder)

Here's the details of each app :

## Generator

I want to enter data via a very simple web UI. Basically, the user can enter the stock symbol and the price.

The generator will serialize the object in JSON and send it to a socket.

## Spark processor

The Spark processor uses an embedded Spark instance. It will consume the socket and set up a streaming pipeline to create the alert when necessary. The alert is displayed on the console. I will contain the event time, the stock symbol, the price, and the previous price. Do not send an alert if ne previous price was defined.

## Flink processor

The Flink processor does exactly the same thing as the Spark processor, but with Flink. Here, I would also want to use an embedded Flink instance.
