package epidemic;

/*
 * clcg4.java
 *
 * L'Ecuyer multiple linear congruential generator
 *
 * Ported from C to Java by and Raghav Kapoor and Somik Raha  (5/2005)
 */

public class Clcg4 {
	static final int H = 32768; 
	static final int Maxgen = 100; 
	public static final int InitialSeed = 0;
	public static final int LastSeed = 1;
	public static final int NewSeed = 2;
	static final int SeedType = 3;
	long aw[] = new long[4];
	long avw[] = new long[4];  /* a[j]^{2^w} et a[j]^{2^{v+w}}. */
	long a[] = { 45991, 207707, 138556, 49689},
	            m[]={ 2147483647, 2147483543, 2147483423, 2147483323};

	long Ig[][] = new long[4][Maxgen+1];
	long Lg[][] = new long[4][Maxgen+1];
	long Cg[][] = new long[4][Maxgen+1];
    /* Initial seed, previous seed, and current seed. */

	static short i, j;
		
	private long multModM( long s, long t, long M) {
		/* Returns (s*t) MOD M. Assumes that -M < s < M and -M < t < M. */
		/* See L'Ecuyer and Cote (1991). */
		long R, S0, S1, q, qh, rh, k;
			
		if( s<0) s+=M;
		if( t<0) t+=M;
		if( s<H) { S0=s; R=0;}
		else {
		S1=s/H; S0=s-H*S1;
		qh=M/H; rh=M-H*qh;
		if( S1>=H) {
		S1-=H; k=t/qh; R=H*(t-k*qh)-k*rh;
		while( R<0) R+=M;
		}
		else R=0;
		if( S1!=0) {
		q=M/S1; k=t/q; R-=k*(M-S1*q);
		if( R>0) R-=M;
		R += S1*(t-k*q);
		while( R<0) R+=M;
		}
		k=R/qh; R=H*(R-k*qh)-k*rh;
		while( R<0) R+=M;
		}
		if( S0!=0) {
		q=M/S0; k=t/q; R-=k*(M-S0*q);
		if( R>0) R-=M;
		R+=(S0*(t-k*q));
		while( R<0) R+=M;
		}
		return R;
	}
		
		
	/*---------------------------------------------------------------------*/
	/* Public part. */
	/*---------------------------------------------------------------------*/
	public void setSeed(int generatorID, long seed[]) {
		if( generatorID>Maxgen) System.out.println( "ERROR: SetSeed with g > Maxgen\n");
		for( j=0; j<4; j++) Ig[j][generatorID]=seed[j];
		initGenerator( generatorID, InitialSeed);
	}
		
		
	public void writeState( int generatorID) {
		System.out.println("State of generator g = "+generatorID);
		for( j=0; j<4; j++) System.out.println("Cg["+j+"] = "+Cg[j][generatorID]);
	}
		
		
	public long [] getSeed(int generatorID) {
		long [] seed = new long[4];
		for( j=0; j<4; j++) {
			seed[j]=Cg[j][generatorID];	
		}
		return seed;
	}
		
		
	public void initGenerator(int generatorID, int where) {
		if( generatorID>Maxgen) System.out.println( "ERROR: InitGenerator with g > Maxgen\n");
		for( j=0; j<4; j++) {
			switch (where) {
				case InitialSeed :
				Lg[j][generatorID]=Ig[j][generatorID]; break;
				case NewSeed :
				Lg[j][generatorID]=multModM( aw[j], Lg[j][generatorID], m[j]); break;
				case LastSeed :
				break;
			}
			Cg[j][generatorID]=Lg[j][generatorID];
		}
	}
		
		
	public void setInitialSeed( long s[]) {
		int g;
		
		for( j=0; j<4; j++) Ig[j][0]=s[j];
		initGenerator( 0, InitialSeed);
		for( g=1; g<=Maxgen; g++) {
			for( j=0; j<4; j++) Ig[j][g]=multModM( avw[j], Ig[j][g-1], m[j]);
			initGenerator( g, InitialSeed);
		}
	}
		
		
	public void init( long v, long w) {
		long sd[]={11111111, 22222222, 33333333, 44444444};
		
		for( j=0; j<4; j++) {
		for( aw[j]=a[j], i=1; i<=w; i++) aw[j]=multModM( aw[j], aw[j], m[j]);
			for( avw[j]=aw[j], i=1; i<=v; i++) avw[j]=multModM( avw[j], avw[j], m[j]);
		}
		setInitialSeed (sd);
	}
		
		
	public double nextValue(int generatorID) {
		long k,s;
		double u=0.0;
		
		if( generatorID>Maxgen) System.out.println( "ERROR: Genval with g > Maxgen\n");
		
		s=Cg[0][generatorID]; k=s/46693;
		s=45991*(s-k*46693)-k*25884;
		if( s<0) s+=2147483647; 
		Cg[0][generatorID]=s;
		u+=(4.65661287524579692e-10*s);
		
		s=Cg[1][generatorID]; k=s/10339;
		s=207707*(s-k*10339)-k*870;
		if( s<0) s+=2147483543;  
		Cg[1][generatorID]=s;
		u-=(4.65661310075985993e-10*s);
		if( u<0) u+=1.0;
		
		s=Cg[2][generatorID]; k=s/15499;
		s=138556*(s-k*15499)-k*3979;
		if( s<0.0) s+=2147483423;  
		Cg[2][generatorID]=s;
		u+=(4.65661336096842131e-10*s);
		if( u>=1.0) u-=1.0;
		
		s=Cg[3][generatorID]; k=s/43218;
		s=49689*(s-k*43218)-k*24121;
		if( s<0) s+=2147483323;  
		Cg[3][generatorID]=s;
		u-=(4.65661357780891134e-10*s);
		if( u<0) u+=1.0;
		
		return (u);
	}

	public void initDefault() {
		init( 31, 41);
	}
}

