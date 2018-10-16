# k_means_clustering
A clustering algorithm based on k means clustering is developed for clustering objects based on sparse high dimentinal vectors.

Components:

1) Data Set - Reuters-21578 Text Categorization Collection Data Set is used.

2) Selecting Subset of the Data Set for Clustering - Only articles containing a single topic are selected from the .sgm files of the data
set. The text in the body of these articles is used for clustering.

3) Preprocessing the dataset to convert it to a sparse Representation.

4) Form a vector representation of the processed data set.

A partitional clustering algorithm is developed based on follwing two implementation - 

A) Standard Sum of Squares Error Criterion Function for k means.
B) Spherical k means : I2 Criterion Function

Langauges Used:

1) Python is used for data pre processing, i.e. subset selection from data and sparse vector representation.
    
    Package Used: Beautiful Soup
    
2) Java is used for implementing the k means based clustering algorithm.
