import os
import glob
import codecs
import math
from bs4 import BeautifulSoup
import nltk
from nltk.stem import PorterStemmer
from nltk.tokenize import sent_tokenize, word_tokenize
from collections import Counter


stopfile = open("reuters21578/stoplist.txt", "r")
stoplist = stopfile.read()
stopfile.close()
stoplist = stoplist.split()

with open("Selected_Subset.txt") as fp:
        soup = BeautifulSoup(fp,"html.parser")

freq_map = dict()
freq_map_all_articles = list()

for reuter in soup.find_all('reuters'):
        with open("reuters21578.class", "a") as myfile:
            if reuter.body.string is not None:
                newid = reuter.newid.string                          
                topic = reuter.topic.string
                myfile.write(newid+","+topic+"\n");
                content = reuter.body.text
                
                content2 = content.replace('\\n',' ')
                content3 = content2.replace("b'",'')
                content3 = content3.lower()
                import re
                content3 = re.sub('[^0-9a-zA-Z]+', ' ', content3)

                token = content3.split();
                
                ps = PorterStemmer()

                token_list = [ps.stem(w) for w in token]                
               
                token_list = [item for item in token_list if not item.isdigit()]

                freq_map_article = dict()
                       
                for word in token_list:  
                    if word in stoplist:
                        token_list.remove(word)
                    else:
                        if str(word) in freq_map:
                                freq_map[str(word)] = freq_map[str(word)] +1
                        else:
                                freq_map[str(word)] = 1

                        if str(word) in freq_map_article:
                                freq_map_article[str(word)] = freq_map_article[str(word)] +1
                        else:
                                freq_map_article[str(word)] = 1
                                
                freq_map_all_articles.append((newid,freq_map_article))

                token_list = str(token_list)               

        myfile.close()

final_freq_map = dict()
dim_no = 1

index_file = open("reuters21578.clabel", "w")
for key,value in freq_map.items():
        if value>=5:
                final_freq_map[key] = dim_no
                index_file.write(key + '\n')
                dim_no = dim_no+1
index_file.close()

vect_rep1 = open("freq.csv", "w")
vect_rep2 = open("sqrtFreq.csv","w")
vect_rep3 = open("logFreq.csv","w")

for freq_map_l in freq_map_all_articles:
        newid = freq_map_l[0]
        freq_map_article = freq_map_l[1]
        for key,value in freq_map_article.items():
                if key in final_freq_map:
                        vect_rep1.write(newid + "," + str(final_freq_map[key]) + "," + str(value) + "\n")
                        vect_rep2.write(newid + "," + str(final_freq_map[key]) + "," + str(math.sqrt(value)) + "\n")
                        vect_rep3.write(newid + "," + str(final_freq_map[key]) + "," + str(math.log(value,2)) + "\n")
vect_rep1.close()
vect_rep2.close()
vect_rep3.close()


fp.close()

