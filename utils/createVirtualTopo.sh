python ovxctl.py -n createNetwork tcp:localhost:20000 10.0.0.0 16
python ovxctl.py -n createSwitch 1 00:00:00:00:00:00:00:01
python ovxctl.py -n createSwitch 1 00:00:00:00:00:00:00:02
python ovxctl.py -n createPort 1 00:00:00:00:00:00:00:01 1
python ovxctl.py -n createPort 1 00:00:00:00:00:00:00:01 2
python ovxctl.py -n createPort 1 00:00:00:00:00:00:00:01 3
python ovxctl.py -n createPort 1 00:00:00:00:00:00:00:02 1
python ovxctl.py -n createPort 1 00:00:00:00:00:00:00:02 2
python ovxctl.py -n connectLink 1 00:a4:23:05:00:00:00:01 3 00:a4:23:05:00:00:00:02 1 spf 1
python ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:01 1 00:00:00:00:00:01
python ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:01 2 00:00:00:00:00:02
python ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:02 2 00:00:00:00:00:03
python ovxctl.py -n startNetwork 1
