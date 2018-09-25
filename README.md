#Mining Yelp Reviews

## Running Source code

1.  src/Filtering.ipynb - iPython notebook includes code to filter the data with restaurant business data.
2.  src/TopicModelingFilesGen.ipynb - Clean the review text corpus by removing stopwords, punctuations and additional spaces. Also generates a document set for the reviews. The files genrated are used for topic modeling.
3.  src/Yelp_Visualizations.ipynb - various data analysis performed on original and intermediate results.
4.  src/TopicModeling.ipynb - Topic modeling using scikit on sample set of data for initial analysis. For full dataset, mallet was used to perform topic modeling for speed.
    LDA_execition_log - shows the execution steps and logs for topic modeling using mallet.
5.  review_clustering/DataTransfer.jar - includes scala with spark code to perform various joins and aggregation based on topic modeling results. We can execute the code by setting the Sparka and Scala paths in Makefile and executing make all.
6.  src/restaurant_recommendation.ipynb - includes code that is used to recommend restaurants to users based on their preference scala.
7.  src/input - includes files required to execute restaurant_recommendation.ipynb
8.  src/review_clustering/input files to execute review_clustering/src files can be found in this link - https://drive.google.com/open?id=1s8SAU7q41cHwVJTNUXUayVJv4PNLfgwV

