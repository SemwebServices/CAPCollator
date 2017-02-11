curl -XPUT 'localhost:9200/alertssubscriptions/alertsubscription/1/_create?pretty' -H 'Content-Type: application/json' -d'
{
	"recid": "country-ae-city-swic1190-lang-en",
	"name": "Official Public alerts for Dubai in country-ae, in English",
	"shortcode": "country-ae-city-swic1190-lang-en",
	"subshape": {
		"type": "polygon",
		"coordinates":[ [
			[54.8833, 24.7833],
			[55.55, 24.7833],
			[55.55, 25.35],
			[54.8833, 25.35],
			[54.8833, 24.7833]
		]]
	},
	"subscriptionUrl": null,
	"languageOnly": null,
	"highPriorityOnly": null,
	"officialOnly": null,
	"xPathFilterId": null,
	"xPathFilter": null,
	"areaFilterId": null,
	"loadSubsVersion": "1.1"
}
'
