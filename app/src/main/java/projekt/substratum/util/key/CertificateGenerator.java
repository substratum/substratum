/*
 * Copyright (c) 2016-2017 Projekt Substratum
 * This file is part of Substratum.
 *
 * Substratum is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Substratum is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Substratum.  If not, see <http://www.gnu.org/licenses/>.
 */

package projekt.substratum.util.key;

import org.bouncycastle.x509.X509V3CertificateGenerator;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;

import javax.security.auth.x500.X500Principal;

public class CertificateGenerator {
    public static X509Certificate generateX509Certificate(KeyPair keyPair) {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date(System.currentTimeMillis()));
            // Valid from now
            Date validityBeginDate = calendar.getTime();
            // Until 25 years later
            calendar.add(Calendar.YEAR, 25);
            Date validityEndDate = calendar.getTime();

            // Generate the certificate
            X509V3CertificateGenerator generator = new X509V3CertificateGenerator();
            X500Principal dnName = new X500Principal("CN=substratum");

            generator.setSignatureAlgorithm("SHA256WithRSAEncryption");
            generator.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
            generator.setSubjectDN(dnName);
            generator.setIssuerDN(dnName);
            generator.setNotBefore(validityBeginDate);
            generator.setNotAfter(validityEndDate);
            generator.setPublicKey(keyPair.getPublic());

            return generator.generate(keyPair.getPrivate());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
