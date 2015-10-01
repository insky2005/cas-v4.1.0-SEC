#!/bin/bash

branch=$1
echo $branch
echo "Push to osc $branch"
git push osc $branch
echo "Push to github $branch"
git push github $branch

