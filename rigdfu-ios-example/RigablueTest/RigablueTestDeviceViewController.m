//
//  RigablueTestDeviceVewControllerViewController.m
//  RigablueTest
//
//  Created by Eric P. Stutzenberger on 7/14/14.
//  Copyright (c) 2014 Rigado, LLC. All rights reserved.
//

#import "RigablueTestDeviceViewController.h"
#import "Rigablue/RigFirmwareUpdateManager.h"

BOOL sentStmImage;
BOOL sentRadioImage;

CBUUID *uartServiceUuid;
CBUUID *txCharUuid;
CBUUID *rxCharUuid;

CBService *uartService;
CBCharacteristic *txChar;
CBCharacteristic *rxChar;

#define VIAWEAR_UPDATE
//#define LUMENPLAY_UPDATE

#define VIAWEAR_SERVICE             @"2995d87f-8c0e-a894-eb4b-5407c5b416a8"
#define VIAWEAR_CONTROL_POINT       @"2995d97f-8c0e-a894-eb4b-5407c5b416a8"

#define LUMENPLAY_SEVICE            @"9a143caf-d775-4cfb-9eca-6e3a9b0f966b"
#define LUMENPLAY_CONTROL_POINT     @"9a143cbe-d775-4cfb-9eca-6e3a9b0f966b"

#if defined(VIAWEAR_UPDATE)
    #define SERVICE_UUID                VIAWEAR_SERVICE
    #define CONTROL_POINT_UUID          VIAWEAR_CONTROL_POINT
    static uint8_t bootloader_command[] = { 0xff };
#elif defined(LUMENPLAY_UPDATE)
    #define SERVICE_UUID                LUMENPLAY_SEVICE
    #define CONTROL_POINT_UUID          LUMENPLAY_CONTROL_POINT
    static uint8_t bootloader_command[] = { 0xff };
#else
    #error Update type is not defined!!
#endif


@interface RigablueTestDeviceViewController () <RigFirmwareUpdateManagerDelegate, RigLeBaseDeviceDelegate, CBPeripheralDelegate>
{
    RigLeBaseDevice *dev;
    
    BOOL isUpdateInProgress;
    float currentProgress;
    NSString *currentStatus;
    UIAlertView * updateCompleteAlertView;
    RigFirmwareUpdateManager *updateManager;
    NSString *textFieldData;
    
#ifdef VIAWEAR_UPDATE
    uint8_t mac_addr[6];
    BOOL isReadingMacAddress;
#endif
}
@end

@implementation RigablueTestDeviceViewController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    // Do any additional setup after loading the view.
    
    isUpdateInProgress = NO;
}

- (void)viewDidAppear:(BOOL)animated
{
    [super viewDidAppear:animated];
    isUpdateInProgress = NO;
    sentStmImage = NO;
    sentRadioImage = NO;
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (IBAction)sendButtonPressed:(id)sender {
    if (!txChar) {
        return;
    }
    
    uint8_t data[20];
    uint8_t len = _sendTextField.text.length;
    if (len > 20)
        len = 20;
    
    memcpy(data, [_sendTextField.text cStringUsingEncoding:NSUTF8StringEncoding], len);
    NSData *temp = [NSData dataWithBytes:data length:len];
    [dev.peripheral writeValue:temp forCharacteristic:txChar type:CBCharacteristicWriteWithoutResponse];
}

- (IBAction)backPressed:(id)sender {
    [self.navigationController popViewControllerAnimated:YES];
}

- (IBAction)firmwareUpdatePressed:(id)sender {
    /* Initialize a new update manager */
    if (isUpdateInProgress) {
        /* Don't allow a new update while one is currently in progress */
        return;
    }
    
    NSData *firmwareImageData;
    NSString *filePath;
    
    // Get firmware image
    filePath =[[NSBundle mainBundle] pathForResource:@"cdi_001_hw" ofType:@"bin"];
    firmwareImageData = [NSData dataWithContentsOfFile:filePath];
    
    updateManager = [[RigFirmwareUpdateManager alloc] init];
    updateManager.delegate = self;
    CBService *service = nil;
    CBCharacteristic *controlPoint = nil;
    CBUUID *serviceUuid = [CBUUID UUIDWithString:SERVICE_UUID];
    CBUUID *controlPointUuid = [CBUUID UUIDWithString:CONTROL_POINT_UUID];
    
    if ([dev.name isEqualToString:@"RigDfu"] || sentStmImage == YES) {
        /* Invoke bootloader here with pointer to binary image of firmware.  Replease NULL Image parameter and ImageSize parameters
           with appropriate values. */
        [updateManager updateFirmware:dev Image:firmwareImageData ImageSize:(uint32_t)firmwareImageData.length activateChar:nil activateCommand:nil activateCommandLen:0];
        isUpdateInProgress = YES;
        return;
    }
    /* Find Control Point for device - Only works for Airkey */
    for (CBService *svc in [dev getSerivceList]) {
        if ([svc.UUID isEqual:serviceUuid]) {
            service = svc;
            break;
        }
    }
             
    if (service != nil) {
        for (CBCharacteristic *characteristic in service.characteristics) {
            if ([characteristic.UUID isEqual:controlPointUuid]) {
                controlPoint = characteristic;
                break;
            }
        }
    }
    
#ifdef VIAWEAR_UPDATE
    /* First we need to store the current mac address */
    [self getMacAddressForDevice:dev];
    
    uint8_t temp[] = { 0xff, 0xff, 0xff, 0xff, 0xff, 0xff };
    if (memcmp(mac_addr, temp, sizeof(temp)) == 0) {
        NSLog(@"MAC Address invalid. Cancelling update!");
        return;
    }
#endif
    
    if (controlPoint != nil) {
        /* Invoke bootloader here with pointer to binary image of firmware.  Replease NULL Image parameter and ImageSize parameters
         with appropriate values. */
        [updateManager updateFirmware:dev Image:firmwareImageData ImageSize:(uint32_t)firmwareImageData.length activateChar:controlPoint activateCommand:bootloader_command activateCommandLen:sizeof(bootloader_command)];
        isUpdateInProgress = YES;
    } else {
        _updateStatus.text = @"No Control Point Found!!!";
    }
}

- (void) getMacAddressForDevice:(RigLeBaseDevice*)device
{
    CBUUID *disUuid = [CBUUID UUIDWithString:@"180A"];
    CBUUID *serialNumberUuid = [CBUUID UUIDWithString:@"2A25"];
    
    CBService *service = nil;
    CBCharacteristic *serialNumChar = nil;
    
    /* Find the characteristic */
    /* Find Control Point for device - Only works for Airkey */
    for (CBService *svc in [device getSerivceList]) {
        if ([svc.UUID isEqual:disUuid]) {
            service = svc;
            break;
        }
    }
    
    if (service != nil) {
        for (CBCharacteristic *characteristic in service.characteristics) {
            if ([characteristic.UUID isEqual:serialNumberUuid]) {
                serialNumChar = characteristic;
                break;
            }
        }
    }
    
    if (serialNumChar == nil) {
        NSLog(@"Could not find serial number characteristic!!  Mac Address not found!");
        memset(mac_addr, 0xff, sizeof(mac_addr));
        return;
    }
    
    NSData *data = serialNumChar.value;
    
    char *bytes = (char*)data.bytes;
    uint8_t index = 0;
    for (int i = 0; i < data.length; i+=3) {
        if (bytes[i] > 0x39) {
            bytes[i] -= 0x07;
        }
        if (bytes[i+1] > 0x39) {
            bytes[i+1] -= 0x07;
        }
        mac_addr[index] = (bytes[i] - 0x30) << 4;
        mac_addr[index] |= (bytes[i+1] - 0x30);
        index++;
    }

    
    //[device.peripheral readValueForCharacteristic:serialNumChar];
}

- (void)setDevice:(RigLeBaseDevice *)device
{
    dev = device;
    dev.delegate = self;
    
    uartServiceUuid = [CBUUID UUIDWithString:@"6E400001-B5A3-F393-E0A9-E50E24DCCA9E"];
    txCharUuid = [CBUUID UUIDWithString:@"6E400002-B5A3-F393-E0A9-E50E24DCCA9E"];
    rxCharUuid = [CBUUID UUIDWithString:@"6E400003-B5A3-F393-E0A9-E50E24DCCA9E"];
    
    for (CBService *service in [dev getSerivceList]) {
        if ([service.UUID isEqual:uartServiceUuid]) {
            uartService = service;
            break;
        }
    }
    
    for (CBCharacteristic *characteristic in uartService.characteristics) {
        if ([characteristic.UUID isEqual:txCharUuid]) {
            txChar = characteristic;
        } else if([characteristic.UUID isEqual:rxCharUuid]) {
            rxChar = characteristic;
        }
    }
    
    if (rxChar) {
        [dev enableNotificationsForCharacteristic:rxChar];
    }
}

- (void)loadProgress
{
    _updateProgress.progress = currentProgress;
}

- (void)loadStatus
{
    _updateStatus.text = currentStatus;
}

- (void)showFinishAlert
{
    NSString *title     = self.navigationItem.title;
    NSString *message   = @"Firmware Updated Successfully!  Please reconnect to your device.";
    updateCompleteAlertView = [[UIAlertView alloc] initWithTitle:title message:message delegate:self cancelButtonTitle:@"OK" otherButtonTitles:nil];
    updateCompleteAlertView.delegate = self;
    [updateCompleteAlertView show];
}

#pragma mark
#pragma mark - FirmwareUpdateManager Delegate Methods
- (void)updateProgress:(float)progress
{
    currentProgress = progress;
    [self performSelectorOnMainThread:@selector(loadProgress) withObject:nil waitUntilDone:YES];
}

- (void)updateStatus:(NSString*)status errorCode:(RigDfuError_t)error
{
    if (error != DfuError_None) {
        NSString *temp = [NSString stringWithFormat:@"%@%d", status, error];
        currentStatus = temp;
    }
    currentStatus = status;
    [self performSelectorOnMainThread:@selector(loadStatus) withObject:nil waitUntilDone:YES];
}

- (void)didFinishUpdate
{
    [self performSelectorOnMainThread:@selector(showFinishAlert) withObject:nil waitUntilDone:YES];
}

#pragma mark - UIAlertViewDelegate Methods
- (void)alertView:(UIAlertView *)alertView didDismissWithButtonIndex:(NSInteger)buttonIndex
{
    if (alertView == updateCompleteAlertView) {
        [self dismissViewControllerAnimated:YES completion:nil];
        isUpdateInProgress = NO;
        [[UIApplication sharedApplication] setIdleTimerDisabled:NO];
    }
}

#pragma mark - RigLeBaseDeviceDelegate methods
- (void)didUpdateValueForCharacteristic:(CBCharacteristic *)characteristic forDevice:(RigLeBaseDevice *)device
{
    CBUUID *serialNumberUuid = [CBUUID UUIDWithString:@"2A25"];
    
    if ([characteristic.UUID isEqual:serialNumberUuid]) {
        NSData *data = characteristic.value;
        
        char *bytes = (char*)data.bytes;
        uint8_t index = 0;
        for (int i = 0; i < data.length; i+=3) {
            mac_addr[index] = (bytes[i] - 0x30) << 4;
            mac_addr[index] |= (bytes[i+1] - 0x30);
            index++;
        }
        
        return;
    }
    if (characteristic != rxChar) {
        return;
    }
    
    NSData *data = characteristic.value;
    
    char *bytes = (char*)data.bytes;
    NSString *temp = [[NSString alloc] initWithCString:bytes encoding:NSASCIIStringEncoding];
    textFieldData = temp;
    [self performSelectorOnMainThread:@selector(updateRxTextField) withObject:nil waitUntilDone:YES];
}

- (void) discoveryDidCompleteForDevice:(RigLeBaseDevice *)device
{
    
}

- (void)updateRxTextField
{
    _receiveTextField.text = textFieldData;
}
@end
