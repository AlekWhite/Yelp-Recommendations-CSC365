import sys
import pandas as pd
import geopandas as gpd
from geopandas import GeoDataFrame
import plotly.express as px
import xml.etree.ElementTree as Xet
import plotly.graph_objects as go

# Tampa, Nashville, Indianapolis, Philadelphia
cityLocation = [(-82.6588, 27.9017), (-86.7816, 36.1627), (-86.15181, 39.7684), (-75.1652, 39.9526)]

def paresPathFromXml(zone, p):
    path = [[], [], []]
    for e in p.findall('node'):

        lat = e.get('lat')
        lng = e.get('lng')
        rng = 3
        if not ((cityLocation[zone][1] - rng < float(lat)) and (float(lat) < cityLocation[zone][1] + rng)):
            continue
        if not ((cityLocation[zone][0] - rng < float(lng)) and (float(lng) < cityLocation[zone][0] + rng)):
            continue

        # read text fields form xml
        name = e.get('nme')
        nOut = ""
        for c in name:
            if c in "ABCDEFGHIJKLMNOPQRSTUVWXYZ abcdefghijklnmopqrstuvwxyz1234567890&%$#@!().,:;'`~":
                nOut += c
        rad = e.get('rad')
        rOut = ""
        for c in rad:
            if c in "ABCDEFGHIJKLMNOPQRSTUVWXYZ abcdefghijklnmopqrstuvwxyz1234567890&%$#@!().,:;'`~":
                rOut += c

        path[0].append(lng)
        path[1].append(lat)
        path[2].append(nOut + "; " + rOut)
    return path

def plotEntireGraph(zone=0):

    # pull allPaths data from path.xml
    #allPaths = [ ([longs] [lats] [names] [weight]), ([] [] []), . . . ]
    allPaths = []
    root = Xet.parse('C:\\Users\\alekw\\IdeaProjects\\CSC365A1\\gui\\path.xml').getroot()

    mp = root.find('main_path')
    if mp is not None:
        allPaths.append(paresPathFromXml(zone, mp))

    for p in root.findall('path'):
        allPaths.append(paresPathFromXml(zone, p))

    # draw map
    fig = go.Figure(go.Scattermapbox(
        mode="markers+lines",
        marker={'size': 10}))


    # define traces
    for point in allPaths:
        fig.add_trace(go.Scattermapbox(
            mode="markers+lines",
            lon=point[0],
            lat=point[1],
            hovertext=point[2],
            line={'width': 2},
            marker={'size': 5,
                    'color': '#4e43c4'}))

    if mp is not None:
        point = allPaths[0]
        fig.add_trace(go.Scattermapbox(
            mode="markers+lines",
            lon=point[0],
            lat=point[1],
            hovertext=point[2],
            line={'width': 4},
            marker={'size': 10,
                    'color': '#e61e21'}))

    # defines map
    fig.update_layout(
        showlegend=False,
        margin={'l': 0, 't': 0, 'b': 0, 'r': 0},
        mapbox={'style': "open-street-map",
                'zoom': 10,
                'center': {'lon': cityLocation[zone][0], 'lat': cityLocation[zone][1]}})
    fig.show()
    #fig.write_image("fig2.png", format='png', engine='kaleido')

# python proj/setup.py py2exe
# cd C:\Users\alekw\AppData\Local\Programs\Python\Python39 & python C:\Users\alekw\IdeaProjects\CSC365A1\gui\buildMap.py 3

reg = sys.argv[1]
plotEntireGraph(int(reg))
