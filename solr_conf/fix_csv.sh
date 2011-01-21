#!/bin/sh
FILE=$1
 
if [ -f $FILE ];
then
   echo "Fixing MySQL quotes in CSV File: $FILE..."
   sed -e 's/\\"/""/g' $FILE > $FILE.tmp
   sed -e 's/\\""/\\"/g' $FILE.tmp > $FILE.final
   rm $FILE.tmp
   mv $FILE.final $FILE
else
   echo "File $FILE does not exists"
fi

