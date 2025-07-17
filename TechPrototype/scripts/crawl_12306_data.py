# crawl_12306_data.py
# This script crawls real data from 12306 for stations, trains, stops, and inventory.
# It generates SQL insert statements or CSV files to populate the Mini-12306 database.
# Requirements: pip install requests beautifulsoup4 pandas

import requests
import json
import csv
import datetime
import time
import random
from bs4 import BeautifulSoup

# Headers to mimic browser
HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
    'Accept-Language': 'en-US,en;q=0.5',
    'Referer': 'https://kyfw.12306.cn/otn/leftTicket/init',
}

# Function to get station list from 12306
def get_stations():
    url = 'https://kyfw.12306.cn/otn/resources/js/framework/station_name.js?station_version=1.9001'
    response = requests.get(url, headers=HEADERS)
    if response.status_code != 200:
        print("Failed to fetch stations")
        return []

    # Parse the JS content
    content = response.text
    start = content.find("@") + 1
    end = content.rfind("'")
    station_str = content[start:end]
    stations = {}
    for station in station_str.split('@'):
        if station:
            parts = station.split('|')
            if len(parts) >= 3:
                telecode = parts[2]
                name = parts[1]
                city = parts[5] if len(parts) > 5 else name
                stations[name] = {'telecode': telecode, 'city': city}
    return stations

# Function to get train list between two stations on a date
def get_trains(from_station, to_station, date, stations_map):
    from_tele = stations_map.get(from_station, {}).get('telecode')
    to_tele = stations_map.get(to_station, {}).get('telecode')
    if not from_tele or not to_tele:
        print(f"Telecodes not found for {from_station} or {to_station}")
        return []

    url = f'https://kyfw.12306.cn/otn/leftTicket/query?leftTicketDTO.train_date={date}&leftTicketDTO.from_station={from_tele}&leftTicketDTO.to_station={to_tele}&purpose_codes=ADULT'
    response = requests.get(url, headers=HEADERS)
    print(f"Response status: {response.status_code}")
    print(f"Response text: {response.text[:200]}...")  # Debug print

    if response.status_code != 200:
        print("Failed to fetch trains")
        return []

    try:
        data = response.json()
    except json.JSONDecodeError:
        print("Invalid JSON response")
        return []

    if 'data' not in data or 'result' not in data.get('data', {}):
        print("No data in response")
        return []

    trains = []
    for item in data['data']['result']:
        parts = item.split('|')
        if len(parts) > 10:
            trains.append({
                'train_number': parts[3],
                'start_station': parts[4],  # Telecode
                'end_station': parts[5],
                'departure_time': parts[8],
                'arrival_time': parts[9],
                'duration': parts[10]  # Need to parse
            })
    return trains

# Function to get train stops (detailed route)
def get_train_stops(train_no, date):
    url = f'https://kyfw.12306.cn/otn/czxx/queryByTrainNo?train_no={train_no}&leftTicketDTO.train_date={date}&rand_code='
    response = requests.get(url, headers=HEADERS)
    if response.status_code != 200:
        return []

    try:
        data = response.json()
    except json.JSONDecodeError:
        print("Invalid JSON for stops")
        return []

    if 'data' not in data or not data['data'].get('data'):
        return []

    stops = []
    for stop in data['data']['data']:
        stops.append({
            'station_name': stop['station_name'],
            'arrival_time': stop['arrive_time'],
            'departure_time': stop['start_time'],
            'stopover_time': stop['stopover_time'],
            'station_no': stop['station_no']
        })
    return stops

# Function to generate SQL inserts
def generate_sql(stations, trains, stops):
    sql = []

    # Stations
    for name, info in stations.items():
        sql.append(f"INSERT INTO stations (station_name, city) VALUES ('{name}', '{info['city']}');" )

    # Trains (assuming we map telecodes to station_ids later)
    for t in trains:
        # Placeholder: need actual station_ids
        sql.append(f"INSERT INTO trains (train_number, train_type, start_station_id, end_station_id, departure_time, arrival_time, duration_minutes) VALUES ('{t['train_number']}', 'G', 1, 2, '{t['departure_time']}', '{t['arrival_time']}', 300);" )  # Dummy values

    # Add more for stops, inventory, etc.

    return '\n'.join(sql)

# Main function
def main():
    stations = get_stations()
    print(f"Found {len(stations)} stations")

    # Example: Get trains from Beijing to Shanghai on a date
    date = (datetime.date.today() + datetime.timedelta(days=1)).strftime('%Y-%m-%d')
    from_station = '北京北'  # Use name, map to telecode
    to_station = '上海'      # Use name
    trains = get_trains(from_station, to_station, date, stations)
    print(f"Found {len(trains)} trains")

    # Get stops for first train
    if trains:
        # Need to get internal train_no (not train_number)
        # From references, train_no is different; this might need adjustment.
        # For now, skip or hardcode a known train_no.
        print("Skipping stops due to train_no requirement")
        stops = []

    # Generate SQL
    sql = generate_sql(stations, trains, stops)
    with open('12306_data.sql', 'w', encoding='utf-8') as f:
        f.write(sql)

    print("SQL file generated: 12306_data.sql")

if __name__ == '__main__':
    main() 