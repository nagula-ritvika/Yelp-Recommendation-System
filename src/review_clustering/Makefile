CLASSPATH = "lib/spark-core_2.11-2.2.0.jar:lib/spark-tags_2.11-2.2.0.jar:lib/hadoop-common-2.6.5.jar:lib/hadoop-annotations-2.6.5.jar:lib/jackson-annotations-2.6.5.jar"
TARGET = target
SCALA_HOME = /Users/rashmidwaraka/Desktop/MR/scala-2.11.8
SPARK_HOME = /usr/local/Cellar/apache-spark/2.2.0

#Data Subset
INPUT1 = input/doc_topic.txt
INPUT2 = input/filtered_review.csv
INPUT3 = input/topic_reviews.csv
INPUT4 = input/reviews_rating.csv

OUTPUT = input/

all: clean build run

clean:
	-rm -rf $(TARGET)
	-mkdir $(TARGET)

run: compile
	$(SPARK_HOME)/bin/spark-submit --class org.neu.data_transform.doc_topic_transform --master local --driver-memory 12g DataTransfer.jar $(INPUT1) $(OUTPUT)
	$(SPARK_HOME)/bin/spark-submit --class org.neu.data_transform.review_topic_transform --master local --driver-memory 12g DataTransfer.jar $(INPUT2) $(INPUT3) $(OUTPUT)
	$(SPARK_HOME)/bin/spark-submit --class org.neu.data_transform.topic_rating_transform --master local --driver-memory 12g DataTransfer.jar $(INPUT4) $(OUTPUT)
	$(SPARK_HOME)/bin/spark-submit --class org.neu.recommend.recommendations --master local --driver-memory 12g DataTransfer.jar


compile: clean
	$(SCALA_HOME)/bin/scalac -classpath $(CLASSPATH) -d $(TARGET) src/main/scala/org/neu/**/*.scala

build: compile
	jar cvfm DataTransfer.jar MANIFEST.MF -C target/ .
