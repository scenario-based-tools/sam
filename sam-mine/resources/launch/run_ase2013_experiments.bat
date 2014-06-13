call sam_mine ./experiments_ase2013/crossftp.xes.gz 20 1.0
call sam_mine ./experiments_ase2013/crossftp.xes.gz 14 1.0
call sam_mine ./experiments_ase2013/crossftp.xes.gz 10 1.0

call sam_mine ./experiments_ase2013/columba.xes.gz 40 1.0
call sam_mine ./experiments_ase2013/columba.xes.gz 20 1.0
call sam_mine ./experiments_ase2013/columba.xes.gz 10 1.0
call sam_mine ./experiments_ase2013/columba.xes.gz 10 0.5

call sam_mine_trigger ./experiments_ase2013/crossftp.xes.gz 7 10 1.0
call sam_mine_trigger ./experiments_ase2013/crossftp.xes.gz 17 10 1.0
call sam_mine_trigger ./experiments_ase2013/crossftp.xes.gz 50,51,52 10 1.0

call sam_mine_effect ./experiments_ase2013/crossftp.xes.gz 7 10 1.0
call sam_mine_effect ./experiments_ase2013/crossftp.xes.gz 17 10 1.0
call sam_mine_effect ./experiments_ase2013/crossftp.xes.gz 50,51,52 10 1.0

call sam_mine_trigger ./experiments_ase2013/crossftp.xes.gz 50,51,52 1 0.45
call sam_mine_effect ./experiments_ase2013/crossftp.xes.gz 50,51,52 1 0.45