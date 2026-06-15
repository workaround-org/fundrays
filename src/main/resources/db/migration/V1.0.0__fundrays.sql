
    create table AdminUser (
        createdAt timestamp(6) with time zone not null,
        lastLoginAt timestamp(6) with time zone,
        id uuid not null,
        displayName varchar(255) not null,
        passwordHash varchar(255) not null,
        roles varchar(255) not null,
        username varchar(255) not null unique,
        primary key (id)
    );

    create table Campaign (
        currency varchar(3) not null,
        createdAt timestamp(6) with time zone not null,
        deadline timestamp(6) with time zone,
        goalAmount bigint not null,
        id uuid not null,
        coverImageUrl varchar(255),
        description TEXT,
        slug varchar(255) not null unique,
        status varchar(255) not null check ((status in ('ACTIVE','PAUSED','COMPLETED','ARCHIVED'))),
        title varchar(255) not null,
        primary key (id)
    );

    create table Donation (
        currency varchar(3) not null,
        amount bigint not null,
        confirmedAt timestamp(6) with time zone,
        createdAt timestamp(6) with time zone not null,
        receiptSentAt timestamp(6) with time zone,
        campaign_id uuid not null,
        id uuid not null,
        donorEmail varchar(255),
        donorName varchar(255),
        message varchar(255),
        paymentMethod varchar(255) not null check ((paymentMethod in ('PAYPAL','MOLLIE','STRIPE'))),
        paymentProviderRef varchar(255) unique,
        status varchar(255) not null check ((status in ('PENDING','CONFIRMED','FAILED','REFUNDED'))),
        primary key (id)
    );

    create table OrganizationSettings (
        orgExemptionDate date,
        smtpPort integer,
        id bigint not null,
        adminNotificationEmail varchar(255),
        orgCity varchar(255),
        orgIssuingAuthority varchar(255),
        orgName varchar(255),
        orgPurpose TEXT,
        orgStreet varchar(255),
        orgTaxId varchar(255),
        orgZip varchar(255),
        smtpFrom varchar(255),
        smtpHost varchar(255),
        smtpPassword varchar(255),
        smtpUser varchar(255),
        primary key (id)
    );

    alter table if exists Donation
       add constraint FKrerrhjmo2dl2bvqtdbn9d3va3
       foreign key (campaign_id)
       references Campaign;
