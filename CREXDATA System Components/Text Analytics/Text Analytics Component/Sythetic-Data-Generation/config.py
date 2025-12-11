
MODEL_CONFIG = {
    "name":  "gemma-3-27b-it-Q8_0.gguf", 
    "path": "/path/to/model/",
    "context_length": 16384,
    "generation_params": {                      # Use the generation parameters suggested by the model's developers
        "temperature": 1.5,
        "top_p": 0.1,
        "top_k": 1,
        "min_p": 0.1,
        "repeat_penalty": 1.1,
        "max_tokens": 16384                     # Higher value here will allow generating more tweets per batch
        }
    }


CRISIS_KEYWORDS = {
                    "flood": {
                        "English": "power outage/failure,blocked highway/road/street,rescue boat,shelter,missing persons,\
                            flood,trapped,water,rain,hail,storm,flash flood,evacuate,evacuation",
                        "Catalan": "Carretera/carrer/carretera bloquejada,tall d'energia,inundació,pluja torrencial,riada,\
                            cabal,canal,gota freda,DANA,persona desapareguda,persones desaparegudes,atrapat,refugi",
                        "Spanish": "Carretera/calle/carretera bloqueada,corte de energía,inundación,lluvia torrencial,riada,\
                            caudal,cauce,gota fría,DANA,persona desaparecida,personas desaparecidas,atrapado,refugio",
                        "German": "hochwasser,wasserwacht,gewässer,wassermasse,unwetter,unterwasser,gewitter,sturm,überflutet,\
                            überschwemmt,jahrhunderthochwasser,jahrhundertflut,vollgelaufene keller,heftiger regen,starkregen,\
                                überflutung,überschwemmung,starkregen,hagel,wasserschaden,unwettereinsätze,regenfälle,vermisste personen,\
                                    schlammlawine,eingeschlossen,stromausfall,unwetterkatastrophe,sturzflut,windböen,hochwasserwarnung"
                        
                        },
                    "wildfire": {
                        "English": "missing persons,damage,fire,smoke,mudslides,mudflow,debris flow,forest,wind,tree,firefighters,\
                            rescue,burning,drought,forest,park,flames,woods,emergency,evacuate,evacuation",
                        "Catalan": "incendi,flames,foc,flama,arbre,bosc,forestal,vent,sequera,matollar,crema,cremar,cremant,bomber,\
                            ventós,tramuntana,parc,militar,emergències,colades de fang,esllavissades,persona desapareguda,\
                                persones desaparegudes,fumarada,fum",
                        "Spanish": "incendio,llamas,fuego,llama,árbol,bosque,forestal,viento,sequía,matorral,quema,quemar,quemando,\
                            bombero,ventoso,parque,militar,emergencias,coladas de barro,deslizamiento,persona desaparecida,\
                                personas desaparecidas,humareda,humo",
                        "German": "waldbrand,flurbrand,flamme,rauch,vegetationsbrand,waldfläche,brandgebiet,brandfläche,ausbreitung,\
                            ausgebreitet,heiß,gefahr,problem,probleme,problematisch,wind,hitze,waldbrandfläche,waldbrandgebiet,wüten,\
                                außer kontrolle,unter kontrolle,evakuierung,evakuiert,evakuieren,sicherheit,brennen,brandbekämpfung,\
                                    feuerwehrmänner,flächenbrand,schadenslage,großschadenslage,unmöglich,waldbrände,warnung,warnen,\
                                        rauchsäule,rauchsäulen,vernichtet,alarmierung,alarmiert,ernstfall,waldstück,betroffen,bäume,\
                                            vermisste personen,"
                        }
                    }

OUT_DIR = "/path/to/save/"