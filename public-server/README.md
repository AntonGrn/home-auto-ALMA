# PublicServer

ALMA PublicServer is a service running on a public network. Its purpose is to connect remote AndroidClients to HomeServer instances, without the need to port-forward HomeServers and by providing necessary security for the communicaiton. 

See [main page](LINK) for more information about the PublicServer.

## Database
Below is the database tables required to run ALMA PublicServer.

**Client_Android**
| Column name | Data type | Note |
| --- | --- | --- |
| name | String | PRIM.KEY |
| systemID | Integer | - |
| admin | Boolean | - |
| password | String | - |
| sessionKey | String | - |
| isBanned | Boolean | - |

