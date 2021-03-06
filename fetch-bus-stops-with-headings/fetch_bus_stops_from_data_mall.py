#!/usr/bin/env python
import data_mall
import json

data_mall.authenticate(os.environ.get('DATA_MALL_API_TOKEN'),
                       os.environ.get('DATA_MALL_UUID'))

bus_stops = data_mall.get('BusStops', 0)

json.dump(bus_stops, open('bus_stops_from_data_mall.json', 'w'), indent=2)
