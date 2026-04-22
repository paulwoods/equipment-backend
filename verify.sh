#!/bin/bash
export $(cat ../.env | xargs) && mvn verify

