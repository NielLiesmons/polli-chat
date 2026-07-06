#!/bin/sh

MAIN=./src/main

codespell \
  --skip './.git,./build,$MAIN/res/values-*/strings.xml,./jni/deltachat-core-rust' \
  --ignore-words-list formattings
