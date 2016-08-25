/*
 * Copyright 2014 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

function update(chunk, encoding) {
  this._delegate.update( bufferChunk( chunk, encoding )._rawBuffer() );
}

function bufferChunk(chunk, encoding) {
  if ( Buffer.isBuffer( chunk ) ) {
    return chunk;
  }
  return new Buffer( chunk, encoding );
}


function digest(outputEncoding) {
  var buf = process.binding('buffer').createBuffer( this._delegate.digest() );

  if ( outputEncoding && outputEncoding != 'buffer' ) {
    return buf.toString( outputEncoding );
  }

  return buf;
}

var hashAlgorithms = {
  'md4':       ClassHelpers.getClass('org.bouncycastle.crypto.digests.MD4Digest'),
  'md5':       ClassHelpers.getClass('org.bouncycastle.crypto.digests.MD5Digest'),
  'sha1':      ClassHelpers.getClass('org.bouncycastle.crypto.digests.SHA1Digest'),
  'sha3':      ClassHelpers.getClass('org.bouncycastle.crypto.digests.SHA3Digest'),
  'sha224':    ClassHelpers.getClass('org.bouncycastle.crypto.digests.SHA224Digest'),
  'sha256':    ClassHelpers.getClass('org.bouncycastle.crypto.digests.SHA256Digest'),
  'sha384':    ClassHelpers.getClass('org.bouncycastle.crypto.digests.SHA384Digest'),
  'sha512':    ClassHelpers.getClass('org.bouncycastle.crypto.digests.SHA512Digest'),
  'ripemd120': ClassHelpers.getClass('org.bouncycastle.crypto.digests.RIPEMD128Digest'),
  'ripemd160': ClassHelpers.getClass('org.bouncycastle.crypto.digests.RIPEMD160Digest'),
  'ripemd256': ClassHelpers.getClass('org.bouncycastle.crypto.digests.RIPEMD256Digest'),
  'ripemd320': ClassHelpers.getClass('org.bouncycastle.crypto.digests.RIPEMD320Digest'),
  'rmd160':    ClassHelpers.getClass('org.bouncycastle.crypto.digests.RIPEMD160Digest'),
  'whirlpool': ClassHelpers.getClass('org.bouncycastle.crypto.digests.WhirlpoolDigest'),
};

function Hash(algorithm) {
  if ( ! this instanceof Hash ) { return new Hash(algorithm); }

  var algo = hashAlgorithms[ algorithm ];

  if ( ! algo ) {
    throw new Error( "Digest method not supported" );
  }

  this._delegate = new Packages.io.nodyn.crypto.Hash( new algo() );
}

Hash.prototype.update = update;
Hash.prototype.digest = digest;

module.exports.Hash = Hash;
module.exports.getHashes = function() {
  var hashes = [];
  for ( var n in hashAlgorithms ) {
    hashes.push( n );
  }
  return hashes;
};

function Hmac() {
  if ( ! this instanceof Hmac ) { return new Hmac(); }
}

Hmac.prototype.init = function(algorithm, key) {

  var algo = hashAlgorithms[ algorithm ];

  if ( ! algo ) {
    throw new Error( "Digest method not supported " + algorithm );
  }

  this._delegate = new Packages.io.nodyn.crypto.Hmac( new algo(), key._rawBuffer() );
};

Hmac.prototype.update = update;
Hmac.prototype.digest = digest;

module.exports.Hmac = Hmac;

var engines  = ClassHelpers.getClass('org.bouncycastle.crypto.engines');
var modes    = ClassHelpers.getClass('org.bouncycastle.crypto.modes');
var paddings = ClassHelpers.getClass('org.bouncycastle.crypto.paddings');

function cbc(cipher) {
  return new modes.CBCBlockCipher(cipher);
}

function cfb(cipher) {
  return new modes.CFBBlockCipher(cipher);
}

function ecb(cipher) {
  // nothing, just here for consistency
  return cipher;
}

function buffered(cipher) {
  return new ClassHelpers.getClass('org.bouncycastle.crypto.BufferedBlockCipher')( cipher );
}

function pkcs7(cipher) {
  return new paddings.PaddedBufferedBlockCipher( cipher, new paddings.PKCS7Padding() );
}

var cipherAlgorithms = {};

function registerCipher(name, keyLen, ivLen, factory) {
  cipherAlgorithms[name] = {
    keyLen:  keyLen,
    ivLen:   ivLen,
    factory: factory,
  };
}

function aes_cbc() {
  return pkcs7( cbc( new engines.AESEngine() ) );
}

function aes_ecb() {
  return pkcs7( ecb( new engines.AESEngine() ) );
}

registerCipher( 'aes128',      128, 16, aes_cbc );
registerCipher( 'aes-128-cbc', 128, 16, aes_cbc );
registerCipher( 'aes-128-ecb', 128, 0,  aes_ecb );

registerCipher( 'aes192',      192, 16, aes_cbc );
registerCipher( 'aes-192-cbc', 192, 16, aes_cbc );
registerCipher( 'aes-192-ecb', 192, 0,  aes_ecb );

registerCipher( 'aes256',      256, 16, aes_cbc );
registerCipher( 'aes-256-cbc', 256, 16, aes_cbc );
registerCipher( 'aes-256-ecb', 256, 0,  aes_ecb );

function bf_cbc() {
  return pkcs7( cbc( new engines.BlowfishEngine() ) );
}

function bf_ecb() {
  return pkcs7( cbc( new engines.BlowfishEngine() ) );
}

registerCipher( 'bf',     128, 8, bf_cbc );
registerCipher( 'bf-cbc', 128, 8, bf_cbc );
registerCipher( 'bf-ecb', 128, 0, bf_ecb );
registerCipher( 'blowfish', 128, 0, bf_ecb );

function camellia_cbc() {
  return pkcs7( cbc( new engines.CamelliaEngine() ) );
}

function camellia_ecb() {
  return pkcs7( ecb( new engines.CamelliaEngine() ) );
}

registerCipher( 'camellia128',      128, 16, camellia_cbc );
registerCipher( 'camellia-128-cbc', 128, 16, camellia_cbc );
registerCipher( 'camellia-128-ecb', 128, 0,  camellia_ecb );

registerCipher( 'camellia192',      192, 16, camellia_cbc );
registerCipher( 'camellia-192-cbc', 192, 16, camellia_cbc );
registerCipher( 'camellia-192-ecb', 192, 0,  camellia_ecb );

registerCipher( 'camellia256',      256, 16, camellia_cbc );
registerCipher( 'camellia-256-cbc', 256, 16, camellia_cbc );
registerCipher( 'camellia-256-ecb', 256, 0,  camellia_ecb );

function cast5_cbc() {
  return pkcs7( cbc( new engines.CAST5Engine() ) );
}

function cast5_ecb() {
  return pkcs7( ecb( new engines.CAST5Engine() ) );
}

registerCipher( 'cast',      128, 8, cast5_cbc );
registerCipher( 'cast-cbc',  128, 8, cast5_cbc );
registerCipher( 'cast5-cbc', 128, 8, cast5_cbc );
registerCipher( 'cast5-ecb', 128, 0, cast5_ecb );

function des_cbc() {
  return pkcs7( cbc( new engines.DESEngine() ) );
}

function des_ecb() {
  return pkcs7( ecb( new engines.DESEngine() ) );
}

registerCipher( 'des',     64, 8, des_cbc );
registerCipher( 'des-cbc', 64, 8, des_cbc );
registerCipher( 'des-ecb', 64, 0, des_ecb );

function des_ede_cbc() {
  return pkcs7( cbc( new engines.DESedeEngine() ) );
}

function des_ede_ecb() {
  return pkcs7( ecb( new engines.DESedeEngine() ) );
}

registerCipher( 'des-ede',     128, 0, des_ede_ecb );
registerCipher( 'des-ede-cbc', 128, 8, des_ede_cbc );

registerCipher( 'des3',         192, 8, des_ede_cbc );
registerCipher( 'des-ede3',     192, 0, des_ede_ecb );
registerCipher( 'des-ede3-cbc', 192, 8, des_ede_cbc );

function idea_cbc() {
  return pkcs7( cbc( new engines.IDEAEngine() ) );
}

function idea_ecb() {
  return pkcs7( ecb( new engines.IDEAEngine() ) );
}

registerCipher( 'idea',     128, 8, idea_cbc );
registerCipher( 'idea-cbc', 128, 8, idea_cbc );
registerCipher( 'idea-ecb', 128, 0, idea_ecb );

function rc2_cbc() {
  return pkcs7( cbc( new engines.RC2Engine() ) );
}

function rc2_ecb() {
  return pkcs7( ecb( new engines.RC2Engine() ) );
}

registerCipher( 'rc2',     128, 8, rc2_cbc );
registerCipher( 'rc2-cbc', 128, 8, rc2_cbc );
registerCipher( 'rc2-ecb', 128, 0, rc2_ecb );

registerCipher( 'rc2-40-cbc', 40, 8, rc2_cbc );
registerCipher( 'rc2-64-cbc', 64, 8, rc2_cbc );

function seed_cbc() {
  return pkcs7( cbc( new engines.SEEDEngine() ) );
}

function seed_ecb() {
  return pkcs7( ecb( new engines.SEEDEngine() ) );
}

registerCipher( 'seed-cbc', 128, 16, seed_cbc );
registerCipher( 'seed-ecb', 128, 0,  seed_ecb );

function generateKeyIv(password, algo) {
  return new Packages.io.nodyn.crypto.OpenSSLKDF( password._rawBuffer(), algo.keyLen, algo.ivLen );
}

function CipherBase(encipher){
  this._encipher = encipher;
}

CipherBase.prototype.init = function(cipher, password) {
  var algo = cipherAlgorithms[cipher];
  if ( ! algo ) {
    throw new Error( "Unknown cipher: " + cipher );
  }

  var keyIv = generateKeyIv( password, algo );

  this.initiv( cipher,
               process.binding('buffer').createBuffer( keyIv.key ),
               process.binding('buffer').createBuffer( keyIv.iv ) );
};

CipherBase.prototype.initiv = function(cipher, key, iv) {

  var algo = cipherAlgorithms[cipher];
  if ( ! algo ) {
    throw new Error( "Cipher method not supported" );
  }

  this._delegate = new Packages.io.nodyn.crypto.Cipher( this._encipher, algo.factory(), key._rawBuffer(), iv._rawBuffer() );
};


CipherBase.prototype.update = update;

CipherBase.prototype.final = function() {
  return process.binding('buffer').createBuffer( this._delegate.doFinal() );
};

module.exports.CipherBase = CipherBase;

module.exports.getCiphers = function() {
  var ciphers = [];
  for ( var n in cipherAlgorithms ) {
    ciphers.push( n );
  }
  return ciphers;
};

var signatureAlgorithms = {
  'RSA-SHA256': 'SHA256withRSA',
};

function Sign() {
  this._delegate = new Packages.io.nodyn.crypto.Sign();
}

Sign.prototype.init = function(algorithm) {
  var algo = signatureAlgorithms[algorithm];
  if ( ! algo ) {
    throw new Error( "Invalid signature algorithm: " + algorithm );
  }
  this._delegate.init( algo );
};

Sign.prototype.update = update;

Sign.prototype.sign = function(key, junk, passphrase) {
  var ret = this._delegate.sign(key._rawBuffer(), passphrase);
  return process.binding('buffer').createBuffer( ret );
};

module.exports.Sign = Sign;

function Verify() {
  this._delegate = new Packages.io.nodyn.crypto.Verify();
}

Verify.prototype.init = function(algorithm) {
  var algo = signatureAlgorithms[algorithm];
  if ( ! algo ) {
    throw new Error( "Invalid signature algorithm: " + algorithm );
  }
  this._delegate.init( algo );
};

Verify.prototype.update = update;

Verify.prototype.verify = function(object, signature) {
  return this._delegate.verify( object._rawBuffer(), signature._rawBuffer() );
};

module.exports.Verify = Verify;

var blocking = require('nodyn/blocking');


function pbkdf2(password, salt, iterations, keylen, digest, callback) {
  blocking.submit( function() {
    var ret = pbkdf2Sync( password, salt, iterations, keylen, digest );
    blocking.unblock( function() {
      callback( null, ret );
    })();
  });
}

function pbkdf2Sync(password, salt, iterations, keylen, digest) {
  var key = Packages.io.nodyn.crypto.PBKDF2.pbkdf2( password._rawBuffer(), salt._rawBuffer(), iterations, keylen );
  return process.binding('buffer').createBuffer( key );
}

module.exports.PBKDF2 = function(password, salt, iterations, keylen, digest, callback) {
  if ( callback ) {
    pbkdf2(password, salt, iterations, keylen, digest, callback);
  } else {
    return pbkdf2Sync(password, salt, iterations, keylen, digest);
  }
};

function randomBytes(size, callback) {
  blocking.submit( function() {
    var ret = Packages.io.nodyn.crypto.RandomGenerator.random(size);
    ret = process.binding('buffer').createBuffer(ret);
    blocking.unblock( function() {
      callback( null, ret );
    })();
  } );
}

function randomBytesSync(size) {
  return Packages.io.nodyn.crypto.RandomGenerator.random( size );
}

function pseudoRandomBytes(size, callback) {
  blocking.submit( function() {
    var ret = Packages.io.nodyn.crypto.RandomGenerator.pseudoRandom(size);
    ret = process.binding('buffer').createBuffer(ret);
    blocking.unblock( function() {
      callback( null, ret );
    })();
  } );
}

function pseudoRandomBytesSync(size) {
  return Packages.io.nodyn.crypto.RandomGenerator.pseudoRandom( size );
}

module.exports.randomBytes = function(size, callback) {
  if ( callback ) {
    randomBytes(size, callback);
  } else {
    var ret = randomBytesSync(size);
    ret = process.binding('buffer').createBuffer(ret);
    return ret;
  }
};

module.exports.pseudoRandomBytes = function(size, callback) {
  if ( callback ) {
    pseudoRandomBytes(size, callback);
  } else {
    var ret = pseudoRandomBytesSync(size);
    ret = process.binding('buffer').createBuffer(ret);
    return ret;
  }
};

function SecureContext() {
  this._context = new Packages.io.nodyn.crypto.SecureContext();
}

SecureContext.prototype.init = function(secureProtocol) {
  this._context.init(secureProtocol);
};

SecureContext.prototype.setKey = function(key, passphrase) {
  this._context.setKey( key._rawBuffer(), passphrase );
};

SecureContext.prototype.setCert = function(cert) {
  this._context.setCert( cert._rawBuffer() );
};

SecureContext.prototype.addCACert = function(caCert) {
  this._context.addCACert( caCert._rawBuffer() );
};

SecureContext.prototype.setCiphers = function(ciphers) {
  this._context.setCiphers( ciphers );
};

SecureContext.prototype.setECDHCurve = function(ecdhCurve) {
  this._context.setECDHCurve( ecdhCurve );
};

SecureContext.prototype.addRootCerts = function(rootCerts) {
  if ( ! rootCerts ) {
    return;
  }

  for ( var i = 0 ; i < rootCerts.length ; ++i ) {
    this._context.addRootCert( rootCerts[i]._rawBuffer() );
  }
};

SecureContext.prototype.setSessionIdContext = function(sessionIdContext) {
  this._context.setSessionIdContext( sessionIdContext );
};

module.exports.SecureContext = SecureContext;

// ----------------------------------------------------------------------
// ----------------------------------------------------------------------

function DiffieHellman(sizeOrKey, generator) {
  if ( ! ( this instanceof DiffieHellman ) ) {
    return new DiffieHellman(sizeOrKey, generator);
  }

  if ( typeof sizeOrKey == 'number' ) {
    this._dh = new Packages.io.nodyn.crypto.dh.DiffieHellman( sizeOrKey, generator );
  } else {
    var keyBuf = sizeOrKey._rawBuffer();
    this._dh = new Packages.io.nodyn.crypto.dh.DiffieHellman( keyBuf, generator );
  }
}

module.exports.DiffieHellman = DiffieHellman;

DiffieHellman.prototype.setPublicKey = function(key) {
  this._dh.publicKey = key._rawBuffer();
};

DiffieHellman.prototype.setPrivateKey = function(key) {
  this._dh.privateKey = key._rawBuffer();
};

function DiffieHellmanGroup(name) {
  if ( ! ( this instanceof DiffieHellmanGroup ) ) {
    return new DiffieHellmanGroup(name);
  }
  this._dh = new Packages.io.nodyn.crypto.dh.DiffieHellman( name );
}

module.exports.DiffieHellmanGroup = DiffieHellmanGroup;

function dhGetPrime() {
  return process.binding('buffer').createBuffer( this._dh.prime );
}

DiffieHellmanGroup.prototype.getPrime = dhGetPrime;
DiffieHellman.prototype.getPrime = dhGetPrime;


function dhGenerateKeys() {
  var publicBytes = this._dh.generateKeys();
  this._generated = true;
  return process.binding('buffer').createBuffer( publicBytes );
}

DiffieHellman.prototype.generateKeys = dhGenerateKeys;
DiffieHellmanGroup.prototype.generateKeys = dhGenerateKeys;

function dhGetPublicKey() {
  if ( ! this._generated ) {
    throw new Error( "No public key - did you forget to generate one?");
  }

  return process.binding('buffer').createBuffer( this._dh.publicKey );
}

DiffieHellman.prototype.getPublicKey = dhGetPublicKey;
DiffieHellmanGroup.prototype.getPublicKey = dhGetPublicKey;

function dhGetPrivateKey() {
  if ( ! this._generated ) {
    throw new Error( "No private key - did you forget to generate one?");
  }

  return process.binding('buffer').createBuffer( this._dh.privateKey );
}

DiffieHellman.prototype.getPrivateKey = dhGetPrivateKey;
DiffieHellmanGroup.prototype.getPrivateKey = dhGetPrivateKey;

function dhGetGenerator() {
  return process.binding('buffer').createBuffer( this._dh.generator );
}

DiffieHellman.prototype.getGenerator = dhGetGenerator;
DiffieHellmanGroup.prototype.getGenerator = dhGetGenerator;

function dhComputeSecret(other) {
  var secret = this._dh.computeSecret( process.binding('buffer').extractBuffer( other ) );
  return process.binding('buffer').createBuffer( secret );
}

DiffieHellman.prototype.computeSecret = dhComputeSecret;
DiffieHellmanGroup.prototype.computeSecret = dhComputeSecret;



