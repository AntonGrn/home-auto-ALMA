# PublicServer

ALMA PublicServer is a service running on a public network. Its purpose is to connect remote AndroidClients to HomeServer instances, without the need to port-forward HomeServers and by providing necessary security for the communicaiton. 

See [main page](LINK) for more information about the PublicServer.

## Database tables
The database tables required to run ALMA PublicServer are listed below.

Table name: **Client_Android**
| Column name | Data type | Note |
| --- | --- | --- |
| name | String | PRIM.KEY |
| systemID | Integer | - |
| admin | Boolean | - |
| password | String | - |
| sessionKey | String | - |
| banned | Boolean | - |

Table name: **Client_HomeServer**
| Column name | Data type | Note |
| --- | --- | --- |
| systemID | Integer | PRIM.KEY |
| password | String | - |
| banned | Boolean | - |

Table name: **Client_Traffic**
| Column name | Data type | Note |
| --- | --- | --- |
| clientName | String| PRIM.KEY |
| clientType| String | - |
| clientIP | String | PRIM.KEY |
| nbrOfConnections | Integer | - |
| lastConnStart | DateTime | - |
| lastConnClose | DateTime | - |
| firstConn | DateTime | - |



