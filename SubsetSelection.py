import os
import glob
import codecs
from bs4 import BeautifulSoup
from collections import Counter

i = 0
l = list()

path = "reuters21578"

for infile in glob.glob(os.path.join(path, "*.sgm")):
    markup = (infile)
    soup = BeautifulSoup(open(markup, "rb").read())

    for reuter in soup.find_all('reuters'):
        if len(reuter.topics.contents) == 1:
            l.insert(i,reuter.topics.string)
            i = i+1

#print(i)

common_topics = [word for word, word_count in Counter(l).most_common(20)]

#print(common_topics)
        
j = 0

for i in range(22):
    if i < 10:
        filename = "reuters21578/reut2-00" + str(i) + ".sgm"
    else:
        filename = "reuters21578/reut2-0" + str(i) + ".sgm"

    #with open(filename) as fp:
    with codecs.open(filename, "r",encoding='utf-8', errors='ignore') as fp:
        soup = BeautifulSoup(fp, "html.parser")
            
    for reuter in soup.find_all('reuters'):
        if len(reuter.topics.contents) == 1:
            if reuter.topics.string in common_topics:
                with open("Selected_Subset.txt", "a") as myfile:                        
                    if reuter.body is not None:
                        newid = reuter.get('newid')                        
                        myfile.write("<reuters>\n<newid>" + newid + "</newid>")
                        topic = reuter.topics.string
                        myfile.write("\n<topic>" + topic + "</topic>\n")
                        content = (reuter.body.string).encode('utf-8','ignore').strip()
                        content = str(content.decode("ascii","ignore").encode("ascii"))
                                               
                        myfile.write("<body>" + content + "</body>\n</reuters>\n")
                        
                myfile.close()
                j = j+1

#print(j)

        




                
