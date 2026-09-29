#!/bin/bash
for i in {1..20}; do
  if curl -s -o /dev/null http://localhost:80/; then exit 0; fi
  sleep 5
done
exit 1