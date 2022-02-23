package services.ti;

import java.util.NoSuchElementException;

/**
 * EmailAddressExistsException is thrown if an email address is already taken by another account
 * when a TI tries to create a new account for a client using the email address.
 */
public class EmailAddressExistsException extends NoSuchElementException {}
