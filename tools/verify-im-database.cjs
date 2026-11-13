// Host SQLite checks of the actual migration/DAO SQL. Android Room tests remain separate.
const {DatabaseSync}=require('node:sqlite');
const fs=require('fs'),path=require('path'),assert=require('node:assert/strict');
const root=path.resolve(__dirname,'..');
const schemaDir=path.join(root,'app/schemas/com.huanchengfly.tieba.post.database.AppDatabase');
const src=path.join(root,'app/src/main/java/com/huanchengfly/tieba/post');
const migrationSource=fs.readFileSync(path.join(src,'database/DatabaseModule.kt'),'utf8');
const daoSource=fs.readFileSync(path.join(src,'database/ImSessionDao.kt'),'utf8');
function schema(v){return JSON.parse(fs.readFileSync(path.join(schemaDir,`${v}.json`),'utf8')).database;}
function create(v){const db=new DatabaseSync(':memory:');for(const e of schema(v).entities){db.exec(e.createSql.replaceAll('${TABLE_NAME}',e.tableName));for(const i of e.indices ?? [])db.exec(i.createSql.replaceAll('${TABLE_NAME}',e.tableName));}return db;}
function migrate(db,from){const block=migrationSource.match(new RegExp(`MIGRATION_${from}_${from+1} = Migration\\(${from}, ${from+1}\\) \\{ db ->([\\s\\S]*?)\\n\\}`));assert(block);for(const m of block[1].matchAll(/db\.execSQL\("((?:[^"\\]|\\.)*)"\)/g))db.exec(JSON.parse('"'+m[1]+'"'));}
function tableShape(db,name){return db.prepare(`PRAGMA table_info(\`${name}\`)`).all().map(r=>({...r}));}
let checks=0;
for(const version of [39,40,41]){
 const db=create(version);
 db.exec("INSERT INTO draft(id,hash,content) VALUES(1,'draft-1','保留草稿')");
 for(let v=version;v<42;v++)migrate(db,v);
 assert.equal(db.prepare("SELECT content FROM draft WHERE hash='draft-1'").get().content,'保留草稿');
 const expected=create(42);
 for(const entity of schema(42).entities)assert.deepEqual(tableShape(db,entity.tableName),tableShape(expected,entity.tableName));
 expected.close();db.close();checks++;
}
const db=create(41);migrate(db,41);
const putSession=db.prepare('INSERT INTO im_session(uid,peerUid,peerName,peerPortrait,lastMsgId,lastMsgTime,lastMsgPreview,lastMsgFromMe,unreadCount,peerReadMsgId) VALUES(?,42,\'peer\',\'\',2,2,\'hello\',0,2,0)');
putSession.run('a');putSession.run('b');
const putMessage=db.prepare("INSERT OR IGNORE INTO im_message(uid,peerUid,groupId,msgId,msgType,content,createTime,fromUid,isMine) VALUES(?,42,6,?,1,'hello',1,42,0)");
assert.equal(putMessage.run('a',1).changes,1);assert.equal(putMessage.run('a',1).changes,0);
assert.equal(putMessage.run('a',2).changes,1);assert.equal(putMessage.run('b',1).changes,1);checks+=2;
const query=daoSource.match(/@Query\("(UPDATE im_session SET unreadCount = \(SELECT COUNT\(\*\)[^"]+)"\)/)[1];
db.prepare(query).run({uid:'a',peerUid:42,msgId:1});
assert.equal(db.prepare("SELECT unreadCount FROM im_session WHERE uid='a'").get().unreadCount,1);
assert.equal(db.prepare("SELECT unreadCount FROM im_session WHERE uid='b'").get().unreadCount,2);checks+=2;
db.close();console.log(`PASS: ${checks} migration, duplicate-delivery, account-isolation and read-race checks (host SQLite).`);
