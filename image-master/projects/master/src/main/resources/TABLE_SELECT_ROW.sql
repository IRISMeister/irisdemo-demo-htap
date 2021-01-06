Select
	account_id, 
	seqno,
	brokerageaccountnum, 
	org, 
	status, 
	tradingflag, 
	entityaccountnum, 
	clientaccountnum, 
	active_date, 
	topaccountnum, 
	repteamno, 
	repteamname, 
	office_name, 
	region, 
	basecurr, 
	createdby, 
	createdts, 
	group_id, 
	a.load_version_no,
	m.NAME
from SpeedTest.Account a
left outer join SpeedTest.MASTER m on a.load_version_no=m.load_version_no
where account_id=?